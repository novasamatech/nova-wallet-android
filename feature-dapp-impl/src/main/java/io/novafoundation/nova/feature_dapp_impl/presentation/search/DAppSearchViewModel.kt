package io.novafoundation.nova.feature_dapp_impl.presentation.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.net.Uri
import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.analytics.DappStakingPromptAction
import io.novafoundation.nova.analytics.DappStakingPromptKind
import io.novafoundation.nova.analytics.DappStakingPromptPlacement
import io.novafoundation.nova.analytics.StakingFlowSource
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.sendEvent
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_dapp_impl.domain.search.DAppStakingDetection
import io.novafoundation.nova.feature_dapp_impl.domain.search.DAppStakingQueryKind
import io.novafoundation.nova.feature_dapp_impl.domain.search.DAppStakingSiteKind
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.search.DappSearchGroup
import io.novafoundation.nova.feature_dapp_impl.domain.search.DappSearchResult
import io.novafoundation.nova.feature_dapp_impl.domain.search.SearchDappInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.common.dappCategoryToUi
import io.novafoundation.nova.feature_dapp_impl.presentation.search.model.DappSearchModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DAppSearchViewModel(
    private val router: DAppRouter,
    private val resourceManager: ResourceManager,
    private val interactor: SearchDappInteractor,
    private val dappInteractor: DappInteractor,
    private val payload: SearchPayload,
    private val dAppSearchResponder: DAppSearchResponder,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val appLinksProvider: AppLinksProvider,
    private val analyticsService: AnalyticsService
) : BaseViewModel() {

    val dAppNotInCatalogWarning = actionAwaitableMixinFactory.confirmingAction<DappUnknownWarningModel>()

    private val _showStakingNoticeEvent = MutableLiveData<Event<StakingNoticeModel>>()
    val showStakingNoticeEvent: LiveData<Event<StakingNoticeModel>> = _showStakingNoticeEvent

    private var pendingStakingNotice: PendingStakingNotice? = null

    val query = MutableStateFlow(payload.initialUrl.orEmpty())

    // Distinct by kind, so the banner is reported when it appears or changes its kind and not on every typed character
    private val stakingBannerKind = query.map(DAppStakingDetection::queryKind)
        .distinctUntilChanged()
        .onEach(::trackStakingBannerShown)
        .share()

    val stakingBannerVisible = stakingBannerKind.map { it != DAppStakingQueryKind.NONE }
        .distinctUntilChanged()

    private val _selectQueryTextEvent = MutableLiveData<Event<Unit>>()
    val selectQueryTextEvent: LiveData<Event<Unit>> = _selectQueryTextEvent

    private val selectedCategoryId = MutableStateFlow(payload.preselectedCategoryId)

    val categoriesFlow = combine(
        interactor.categories(),
        selectedCategoryId
    ) { categories, categoryId ->
        categories.map { dappCategoryToUi(it, isSelected = it.id == categoryId) }
    }
        .distinctUntilChanged()
        .withSafeLoading()
        .inBackground()
        .share()

    val searchResults = combine(query, selectedCategoryId) { query, categoryId ->
        interactor.searchDapps(query, categoryId)
            .mapKeys { (searchGroup, _) -> mapSearchGroupToTextHeader(searchGroup) }
            .mapValues { (_, groupItems) -> groupItems.map(::mapSearchResultToSearchModel) }
            .toListWithHeaders()
    }
        .inBackground()
        .share()

    init {
        if (!payload.initialUrl.isNullOrEmpty()) {
            _selectQueryTextEvent.sendEvent()
        }

        launch {
            dappInteractor.dAppsSync()
        }
    }

    fun cancelClicked() {
        if (shouldReportResult()) {
            dAppSearchResponder.respond(DAppSearchCommunicator.Response.Cancel)
        }

        router.back()
    }

    private fun mapSearchGroupToTextHeader(searchGroup: DappSearchGroup): TextHeader {
        val content = when (searchGroup) {
            DappSearchGroup.DAPPS -> resourceManager.getString(R.string.dapp_dapps)
            DappSearchGroup.SEARCH -> resourceManager.getString(R.string.common_search)
        }

        return TextHeader(content)
    }

    private fun mapSearchResultToSearchModel(searchResult: DappSearchResult): DappSearchModel {
        return when (searchResult) {
            is DappSearchResult.Dapp -> DappSearchModel(
                title = searchResult.dapp.name,
                description = searchResult.dapp.description,
                icon = searchResult.dapp.iconLink,
                searchResult = searchResult,
                actionIcon = R.drawable.ic_favorite_heart_filled_20.takeIf { searchResult.dapp.isFavourite }
            )

            is DappSearchResult.Search -> DappSearchModel(
                title = searchResult.query,
                searchResult = searchResult,
                actionIcon = null
            )

            is DappSearchResult.Url -> DappSearchModel(
                title = searchResult.url,
                searchResult = searchResult,
                actionIcon = null
            )
        }
    }

    fun stakingBannerClicked() {
        val kind = DAppStakingDetection.queryKind(query.value).toPromptKind() ?: return

        analyticsService.track(
            AnalyticsEvent.DappStakingPromptAction(
                placement = DappStakingPromptPlacement.SEARCH_BANNER,
                kind = kind,
                action = DappStakingPromptAction.GO_TO_STAKING
            )
        )

        openStaking(kind, StakingFlowSource.DAPP_SEARCH_BANNER)
    }

    fun noticeGoToStakingClicked() {
        val notice = consumePendingStakingNotice() ?: return

        trackStakingNoticeAction(notice, DappStakingPromptAction.GO_TO_STAKING)

        openStaking(notice.kind, StakingFlowSource.DAPP_NOTICE)
    }

    fun continueToStakingSiteClicked() {
        val notice = consumePendingStakingNotice() ?: return

        trackStakingNoticeAction(notice, DappStakingPromptAction.CONTINUE_TO_SITE)

        // The Bittensor notice replaces the unknown DApp warning for a TAO-related site
        val isBittensorSite = notice.kind == DappStakingPromptKind.BITTENSOR
        openSearchResult(notice.searchResult, skipUnknownDAppWarning = isBittensorSite)
    }

    fun searchResultClicked(searchResult: DappSearchResult) {
        launch {
            val siteKind = interactor.stakingSiteKind(searchResult)
            val noticeKind = siteKind.toPromptKind()

            if (noticeKind == null) {
                openSearchResult(searchResult, skipUnknownDAppWarning = false)
            } else {
                showStakingNotice(searchResult, siteKind, noticeKind)
            }
        }
    }

    private fun showStakingNotice(searchResult: DappSearchResult, siteKind: DAppStakingSiteKind, noticeKind: DappStakingPromptKind) {
        val host = hostOf(searchResult.targetUrl())

        pendingStakingNotice = PendingStakingNotice(searchResult, noticeKind, host)
        _showStakingNoticeEvent.value = StakingNoticeModel(siteKind, host).event()

        analyticsService.track(
            AnalyticsEvent.DappStakingPromptShown(
                placement = DappStakingPromptPlacement.NOTICE,
                kind = noticeKind,
                dappHost = host
            )
        )
    }

    private fun consumePendingStakingNotice(): PendingStakingNotice? {
        val notice = pendingStakingNotice
        pendingStakingNotice = null

        return notice
    }

    private fun openStaking(kind: DappStakingPromptKind, source: StakingFlowSource) {
        when (kind) {
            DappStakingPromptKind.BITTENSOR -> router.openBittensorStaking(source)
            DappStakingPromptKind.STAKING -> router.openStaking()
        }
    }

    private fun openSearchResult(searchResult: DappSearchResult, skipUnknownDAppWarning: Boolean) = launch {
        val newUrl = searchResult.targetUrl()

        val needsUnknownDAppWarning = !searchResult.isTrustedByNova && !skipUnknownDAppWarning
        if (needsUnknownDAppWarning) {
            dAppNotInCatalogWarning.awaitAction(DappUnknownWarningModel(appLinksProvider.email))
        }

        trackDappOpened(searchResult, newUrl)

        when (payload.request) {
            SearchPayload.Request.GO_TO_URL -> {
                dAppSearchResponder.respond(DAppSearchCommunicator.Response.NewUrl(newUrl))
                router.finishDappSearch()
            }

            SearchPayload.Request.OPEN_NEW_URL -> router.openDAppBrowser(DAppBrowserPayload.Address(newUrl))
        }
    }

    private fun DappSearchResult.targetUrl(): String {
        return when (this) {
            is DappSearchResult.Dapp -> dapp.url
            is DappSearchResult.Search -> searchUrl
            is DappSearchResult.Url -> url
        }
    }

    private fun hostOf(url: String): String = Uri.parse(url).host.orEmpty()

    private fun trackStakingBannerShown(queryKind: DAppStakingQueryKind) {
        val kind = queryKind.toPromptKind() ?: return

        analyticsService.track(
            AnalyticsEvent.DappStakingPromptShown(
                placement = DappStakingPromptPlacement.SEARCH_BANNER,
                kind = kind
            )
        )
    }

    private fun trackStakingNoticeAction(notice: PendingStakingNotice, action: DappStakingPromptAction) {
        analyticsService.track(
            AnalyticsEvent.DappStakingPromptAction(
                placement = DappStakingPromptPlacement.NOTICE,
                kind = notice.kind,
                action = action,
                dappHost = notice.host
            )
        )
    }

    private fun DAppStakingQueryKind.toPromptKind(): DappStakingPromptKind? {
        return when (this) {
            DAppStakingQueryKind.NONE -> null
            DAppStakingQueryKind.STAKING -> DappStakingPromptKind.STAKING
            DAppStakingQueryKind.BITTENSOR -> DappStakingPromptKind.BITTENSOR
        }
    }

    private fun DAppStakingSiteKind.toPromptKind(): DappStakingPromptKind? {
        return when (this) {
            DAppStakingSiteKind.NONE -> null
            DAppStakingSiteKind.THIRD_PARTY_STAKING -> DappStakingPromptKind.STAKING
            DAppStakingSiteKind.BITTENSOR -> DappStakingPromptKind.BITTENSOR
        }
    }

    private fun trackDappOpened(searchResult: DappSearchResult, url: String) {
        val source = when (searchResult) {
            is DappSearchResult.Dapp -> "search"
            is DappSearchResult.Search -> "search"
            is DappSearchResult.Url -> "manual_url"
        }

        analyticsService.track(
            AnalyticsEvent.DappOpened(
                dappHost = hostOf(url),
                source = source,
                isKnownDapp = searchResult.isTrustedByNova
            )
        )
    }

    private fun shouldReportResult() = when (payload.request) {
        SearchPayload.Request.GO_TO_URL -> true

        SearchPayload.Request.OPEN_NEW_URL -> false
    }

    fun onCategoryClicked(id: String) {
        if (selectedCategoryId.value == id) {
            selectedCategoryId.value = null
        } else {
            selectedCategoryId.value = id
        }
    }

    private class PendingStakingNotice(
        val searchResult: DappSearchResult,
        val kind: DappStakingPromptKind,
        val host: String
    )
}
