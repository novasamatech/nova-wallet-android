package io.novafoundation.nova.feature_dapp_impl.presentation.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.sendEvent
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_dapp_impl.domain.search.DAppStakingDetection
import io.novafoundation.nova.feature_dapp_impl.domain.search.DAppStakingKind
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
import kotlinx.coroutines.Job
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

    val stakingNotice = actionAwaitableMixinFactory.create<DAppStakingKind, StakingNoticeAction>()

    private var openSearchResultJob: Job? = null

    val query = MutableStateFlow(payload.initialUrl.orEmpty())

    // Distinct by kind, so the banner is reported when it appears or changes its kind and not on every typed character
    private val stakingBannerKind = query.map(DAppStakingDetection::queryKind)
        .distinctUntilChanged()
        .onEach(::trackStakingBannerShown)
        .share()

    val stakingBannerVisible = stakingBannerKind.map { it != null }
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
        cancelSearchResultOpening()

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
        if (openSearchResultJob?.isActive == true) return

        val kind = DAppStakingDetection.queryKind(query.value) ?: return

        analyticsService.track(
            AnalyticsEvent.DappStakingPromptActionTaken(
                placement = DappStakingPromptPlacement.SEARCH_BANNER,
                kind = kind.toAnalyticsKind(),
                action = DappStakingPromptAction.GO_TO_STAKING
            )
        )

        openStaking(kind, StakingFlowSource.DAPP_SEARCH_BANNER)
    }

    fun searchResultClicked(searchResult: DappSearchResult) {
        if (openSearchResultJob?.isActive == true) return

        openSearchResultJob = launch {
            val details = interactor.resolveSearchResult(searchResult)
            val stakingKind = details.stakingKind

            if (stakingKind == null) {
                openSearchResult(searchResult, details.host, skipUnknownDAppWarning = false)
            } else {
                handleStakingNotice(searchResult, stakingKind, details.host)
            }
        }
    }

    fun cancelSearchResultOpening() {
        openSearchResultJob?.cancel()
    }

    private suspend fun handleStakingNotice(searchResult: DappSearchResult, kind: DAppStakingKind, host: String) {
        analyticsService.track(
            AnalyticsEvent.DappStakingPromptShown(
                placement = DappStakingPromptPlacement.NOTICE,
                kind = kind.toAnalyticsKind(),
                dappHost = host
            )
        )

        val action = stakingNotice.awaitAction(kind)
        trackStakingNoticeAction(kind, action, host)

        when (action) {
            StakingNoticeAction.GO_TO_STAKING -> openStaking(kind, StakingFlowSource.DAPP_NOTICE)
            StakingNoticeAction.CONTINUE_TO_SITE -> openSearchResult(
                searchResult = searchResult,
                host = host,
                skipUnknownDAppWarning = kind == DAppStakingKind.BITTENSOR
            )
        }
    }

    private fun openStaking(kind: DAppStakingKind, source: StakingFlowSource) {
        when (kind) {
            DAppStakingKind.BITTENSOR -> router.openBittensorStaking(source)
            DAppStakingKind.STAKING -> router.openStaking()
        }
    }

    private suspend fun openSearchResult(searchResult: DappSearchResult, host: String, skipUnknownDAppWarning: Boolean) {
        val needsUnknownDAppWarning = !searchResult.isTrustedByNova && !skipUnknownDAppWarning
        if (needsUnknownDAppWarning) {
            dAppNotInCatalogWarning.awaitAction(DappUnknownWarningModel(appLinksProvider.email))
        }

        trackDappOpened(searchResult, host)
        val newUrl = searchResult.targetUrl

        when (payload.request) {
            SearchPayload.Request.GO_TO_URL -> {
                dAppSearchResponder.respond(DAppSearchCommunicator.Response.NewUrl(newUrl))
                router.finishDappSearch()
            }

            SearchPayload.Request.OPEN_NEW_URL -> router.openDAppBrowser(DAppBrowserPayload.Address(newUrl))
        }
    }

    private fun trackStakingBannerShown(kind: DAppStakingKind?) {
        if (kind == null) return

        analyticsService.track(
            AnalyticsEvent.DappStakingPromptShown(
                placement = DappStakingPromptPlacement.SEARCH_BANNER,
                kind = kind.toAnalyticsKind()
            )
        )
    }

    private fun trackStakingNoticeAction(kind: DAppStakingKind, action: StakingNoticeAction, host: String) {
        val analyticsAction = when (action) {
            StakingNoticeAction.GO_TO_STAKING -> DappStakingPromptAction.GO_TO_STAKING
            StakingNoticeAction.CONTINUE_TO_SITE -> DappStakingPromptAction.CONTINUE_TO_SITE
        }

        analyticsService.track(
            AnalyticsEvent.DappStakingPromptActionTaken(
                placement = DappStakingPromptPlacement.NOTICE,
                kind = kind.toAnalyticsKind(),
                action = analyticsAction,
                dappHost = host
            )
        )
    }

    private fun DAppStakingKind.toAnalyticsKind(): DappStakingPromptKind {
        return when (this) {
            DAppStakingKind.STAKING -> DappStakingPromptKind.STAKING
            DAppStakingKind.BITTENSOR -> DappStakingPromptKind.BITTENSOR
        }
    }

    private fun trackDappOpened(searchResult: DappSearchResult, host: String) {
        val source = when (searchResult) {
            is DappSearchResult.Dapp -> "search"
            is DappSearchResult.Search -> "search"
            is DappSearchResult.Url -> "manual_url"
        }

        analyticsService.track(
            AnalyticsEvent.DappOpened(
                dappHost = host,
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
}
