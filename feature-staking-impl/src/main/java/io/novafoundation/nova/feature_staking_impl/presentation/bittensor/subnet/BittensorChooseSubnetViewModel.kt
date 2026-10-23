package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.analytics.BittensorScreen
import io.novafoundation.nova.analytics.SubnetListSource
import io.novafoundation.nova.analytics.SubnetSortKey
import kotlinx.coroutines.flow.drop
import io.novafoundation.nova.analytics.BittensorDataEndpoint
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorReadAnalytics
import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.analytics.StakingAnalyticsSelectionMethod
import io.novafoundation.nova.analytics.AnalyticsService

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.domain.isError
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.fallbackIcon
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.view.input.chooser.ListChooserMixin
import io.novafoundation.nova.common.view.input.chooser.selectedValue
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetBrowseRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetFavorites
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioRowInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRecommendedYieldInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetReference
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetSorting
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorWeeklyPriceSummary
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.passesBittensorThinPoolFilter
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.sortBittensorSubnets
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatToken
import java.math.BigDecimal
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class BittensorChooseSubnetViewModel(
    private val analytics: AnalyticsService,
    private val bittensorSubnetInteractor: BittensorSubnetInteractor,
    private val router: StakingRouter,
    assetIconProvider: AssetIconProvider,
    private val tokenFormatter: TokenFormatter,
    private val retryController: BittensorRetryController,
    private val browseRepository: BittensorSubnetBrowseRepository,
    private val recommendedYields: BittensorRecommendedYieldInteractor,
    private val rowInteractor: BittensorPortfolioRowInteractor,
    private val resources: ResourceManager,
    listChooserMixinFactory: ListChooserMixin.Factory,
    private val payload: BittensorChooseSubnetPayload,
    private val validatorCommunicator: BittensorSelectValidatorCommunicator
) : BaseViewModel() {

    private var isSearchTracked = false

    // Only the picker opened from the staking start (see all options) reports abandonment, not the one from the amount screen.
    private val tracksAbandonment = !payload.replaceAmount

    // Subnet details opened from here report the abandonment themselves or finish the flow, so the picker reports nothing while they are open.
    private var detailsOpen = false

    private val subnetsReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.SUBNETS, BittensorScreen.SUBNET_SELECT)
    private val validatorsReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.VALIDATORS, BittensorScreen.SUBNET_SELECT)
    private val priceServerReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.PRICE_SERVER, BittensorScreen.SUBNET_SELECT)

    val retryEvents = retryController.events

    private var selectionState = payload.selectionState

    val sortingMixin = listChooserMixinFactory.create(
        coroutineScope = viewModelScope,
        dataProvider = {
            val options = listOf(
                BittensorSubnetSorting.POOL_DEPTH,
                BittensorSubnetSorting.AGE,
                BittensorSubnetSorting.APY,
                BittensorSubnetSorting.SEVEN_DAY_CHANGE,
                BittensorSubnetSorting.NAME
            ).map { ListChooserMixin.Model(it, sortingDisplay(it)) }
            ListChooserMixin.Data(options, options.first { it.value == payload.browseState.sorting })
        },
        selectorTitleRes = R.string.common_sort_by
    )

    val hideThinPools = MutableStateFlow(payload.browseState.hideThinPools)

    private val catalogueState = retryController.observe(
        step = "subnet catalogue",
        analytics = subnetsReads,
        transform = { it.withSafeLoading() }
    ) {
        flowOf { browseRepository.subnets() }
    }
        .shareInBackground()

    private val catalogue = catalogueState.filterLoaded()
        .onStart { emit(emptyList()) }
        .shareInBackground()

    private val query = MutableStateFlow("")

    private val assetState = retryController.observe(
        step = "native asset",
        transform = { it.withSafeLoading() },
        source = bittensorSubnetInteractor::observeAsset
    )
        .shareInBackground()

    private val asset = assetState.filterLoaded()
        .shareInBackground()

    val filterDescription = asset.map {
        resources.getString(R.string.bittensor_thin_pool_description, it.token.configuration.symbol.value)
    }
        .shareInBackground()

    val isLoading = combine(catalogueState, assetState) { catalogue, asset -> catalogue.isLoading() || asset.isLoading() }
        .shareInBackground()

    private val hasError = combine(catalogueState, assetState) { catalogue, asset -> catalogue.isError() || asset.isError() }
        .shareInBackground()

    private val rootIcon = asset.map {
        assetIconProvider.getAssetIconOrFallback(it.token.configuration)
    }
        .onStart { emit(AssetIconProvider.fallbackIcon) }
        .shareInBackground()

    private val favorites = bittensorSubnetInteractor.observeFavorites()
        .shareInBackground()

    private val yields = sortingMixin.selectedValue.flatMapLatest { sorting ->
        if (sorting != BittensorSubnetSorting.APY) {
            flowOf { emptyMap<BittensorNetuid.Subnet, BigDecimal>() }
        } else {
            retryController.observe(
                step = "subnet APY",
                analytics = validatorsReads,
                transform = { source ->
                    source.onStart { emit(emptyMap()) }.catch { cause ->
                        if (cause is CancellationException) throw cause
                        emit(emptyMap())
                    }
                }
            ) {
                flowOf { recommendedYields.maximumYields() }
            }
        }
    }
        .shareInBackground()

    // Null without a native price id. A subnet missing from the map has no market listing; a null summary is listed without a computable change.
    private val weeklySummariesState = combine(catalogue, asset) { catalogue, asset ->
        WeeklySummaryRequest(asset.token.configuration.priceId, catalogue.map { it.id }.toSet())
    }
        .distinctUntilChanged()
        .flatMapLatest { request ->
            retryController.observe(
                step = "subnet weekly changes",
                analytics = priceServerReads,
                transform = { it.withSafeLoading() }
            ) {
                flowOf<WeeklySummaries?> {
                    request.nativePriceId?.let { rowInteractor.weeklySummaries(it, request.subnetIds) }
                }
            }
        }
        .shareInBackground()

    private val weekly = weeklySummariesState.map { state ->
        state.dataOrNull.orEmpty().mapNotNull { (subnet, summary) -> summary?.let { subnet to it.changePercent } }.toMap()
    }
        .shareInBackground()

    private val orderedCatalogue = combine(catalogue, sortingMixin.selectedValue, favorites, yields, weekly, ::BrowseInput)
        .runningFold(emptyList<BittensorSubnet>()) { previous, input -> sortCatalogue(previous, input) }
        .shareInBackground()

    private val filteredCatalogue = combine(orderedCatalogue, hideThinPools, favorites) { catalogue, hideThin, favorites ->
        catalogue.filter { !hideThin || passesBittensorThinPoolFilter(it, favorites) }
    }
        .shareInBackground()

    private val subnets = combine(filteredCatalogue, query, favorites, asset, weeklySummariesState, ::mapSubnetRows)
        .onStart { emit(emptyList()) }
        .shareInBackground()

    // Only a search that matches nothing in the whole catalogue hides the root row and the sort/filter/count row.
    // When matches exist but the filters hide them, the controls stay so the user can turn the filters off.
    private val emptyKind = combine(orderedCatalogue, subnets, isLoading, hasError, query) { catalogue, subnets, loading, failed, query ->
        bittensorSubnetEmptyKind(
            loading = loading,
            failed = failed,
            query = query,
            catalogueSize = catalogue.size,
            queryMatches = catalogue.count { it.matchesQuery(query) },
            shown = subnets.size
        )
    }
        .distinctUntilChanged()
        .shareInBackground()

    private val searchHasNoMatches = emptyKind.map { it == BittensorSubnetEmptyKind.NO_SEARCH_MATCHES }
        .distinctUntilChanged()
        .shareInBackground()

    val listState = combine(rootIcon, subnets, isLoading, searchHasNoMatches) { icon, subnets, loading, noMatches ->
        val visible = if (loading) emptyList() else subnets
        BittensorSubnetListState(
            items = bittensorSubnetList(icon, visible, searchHasNoMatches = noMatches),
            loading = loading
        )
    }
        .distinctUntilChanged()
        .shareInBackground()

    val controlsVisible = searchHasNoMatches.map { !it }
        .shareInBackground()

    // Sorting and filtering act on the catalogue, so they stay disabled until it is loaded (as on iOS).
    val controlsEnabled = catalogueState.map { it is ExtendedLoadingState.Loaded }
        .distinctUntilChanged()
        .shareInBackground()

    val subnetCount = combine(orderedCatalogue, hideThinPools, favorites, query, catalogueState) { ordered, hideThin, favorites, query, state ->
        // Count only once the sorted list is the loaded catalogue, so neither "0 of 0" nor "0 of N" flashes while it settles.
        if (state is ExtendedLoadingState.Loaded && ordered.size == state.data.size && ordered.toSet() == state.data.toSet()) {
            val shown = ordered.count { it.matchesQuery(query) && (!hideThin || passesBittensorThinPoolFilter(it, favorites)) }
            resources.getString(R.string.bittensor_subnet_count, shown, state.data.size)
        } else {
            resources.getString(R.string.bittensor_subnet_count_loading)
        }
    }
        .shareInBackground()

    val isEmpty = emptyKind.map { it != BittensorSubnetEmptyKind.NONE }
        .shareInBackground()

    val controlsVisible = searchHasNoMatches.map { !it }
        .shareInBackground()

    val emptyMessage = combine(emptyKind, query) { kind, query ->
        when (kind) {
            BittensorSubnetEmptyKind.NO_SEARCH_MATCHES -> resources.getString(R.string.bittensor_subnets_search_no_matches, query.trim())
            BittensorSubnetEmptyKind.FILTERED_OUT -> resources.getString(R.string.bittensor_subnet_filter_empty)
            BittensorSubnetEmptyKind.NONE, BittensorSubnetEmptyKind.NO_SUBNETS -> resources.getString(R.string.bittensor_no_subnets)
        }
    }
        .shareInBackground()

    init {
        analytics.track(
            BittensorAnalyticsEvent.SubnetListOpened(
                if (payload.replaceAmount) SubnetListSource.SEE_ALL_SUBNETS else SubnetListSource.SEE_ALL_OPTIONS,
                payload.browseState.sorting.analyticsSort(),
                hideThinPools.value
            )
        )
        sortingMixin.selectedValue.distinctUntilChanged().drop(1).onEach {
            analytics.track(BittensorAnalyticsEvent.SubnetSort(it.analyticsSort()))
        }.launchIn(this)
        hideThinPools.drop(1).onEach {
            analytics.track(BittensorAnalyticsEvent.SubnetFilter(it))
        }.launchIn(this)
        validatorCommunicator.responseFlow
            .onEach {
                detailsOpen = false
                selectionState = selectionState.withResponse(it)
            }
            .launchIn(this)
    }

    fun search(query: String) {
        if (query.isNotBlank() && !isSearchTracked) {
            isSearchTracked = true
            analytics.track(BittensorAnalyticsEvent.SubnetSearch())
        }
        this.query.value = query.trim()
    }

    fun select(row: BittensorSubnetRowUi) = launchUnit {
        rememberBrowseState()
        detailsOpen = true
        router.openBittensorSubnetDetails(
            BittensorSubnetDetailsPayload(
                row.subnet.id.value,
                payload.replaceAmount,
                replaceOrigin = payload.replaceOrigin,
                selectionState = selectionState
            )
        )
    }

    fun filterResult(hideThinPools: Boolean) = combine(orderedCatalogue, query, favorites) { subnets, query, favorites ->
        val count = subnets.count { subnet ->
            subnet.matchesQuery(query) && (!hideThinPools || passesBittensorThinPoolFilter(subnet, favorites))
        }
        resources.getString(R.string.bittensor_show_subnets, count)
    }

    fun toggleFavorite(row: BittensorSubnetRowUi) = launchUnit {
        try {
            bittensorSubnetInteractor.toggleFavorite(row.metaId, row.subnet)
            analytics.track(BittensorAnalyticsEvent.SubnetFavorite(!row.favorite))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showError(e)
        }
    }

    fun rootStaking() = launchUnit {
        analytics.track(
            BittensorAnalyticsEvent.TypeSelected(
                BittensorAnalyticsEvent.NETWORK,
                StakingAnalyticsTarget.ROOT,
                StakingAnalyticsSelectionMethod.PICKER_ROOT_ROW,
                0
            )
        )
        selectionState = selectionState.copy(flowContinued = true)
        rememberBrowseState()
        when (bittensorPickerRootAction(payload)) {
            // The picker sits on the root setup it was opened from: return to it instead of stacking another one.
            BittensorPickerRootAction.RETURN -> {
                validatorCommunicator.respond(BittensorSelectValidatorCommunicator.Response(null, null, selectionState))
                router.back()
            }
            BittensorPickerRootAction.REPLACE_SUBNET_AMOUNT -> router.replaceBittensorSubnetAmountWithRoot(
                selectionState,
                quickPick = payload.quickPick
            )
            BittensorPickerRootAction.OPEN -> router.openBittensorRootStaking(selectionState = selectionState)
        }
    }

    fun back() = launchUnit {
        rememberBrowseState()
        validatorCommunicator.respond(BittensorSelectValidatorCommunicator.Response(null, null, selectionState))
        router.back()
    }

    override fun onCleared() {
        if (tracksAbandonment && !detailsOpen && !selectionState.flowContinued) {
            analytics.track(BittensorAnalyticsEvent.Abandoned(BittensorScreen.SUBNET_SELECT, null))
        }
        super.onCleared()
    }

    private suspend fun rememberBrowseState() {
        val browseState = BittensorSubnetBrowseState(sortingMixin.selectedValue.first(), hideThinPools.value)
        selectionState = selectionState.copy(browseState = browseState)
    }

    private fun mapSubnetRows(
        subnets: List<BittensorSubnet>,
        query: String,
        favorites: BittensorSubnetFavorites,
        asset: Asset,
        weeklySummaries: ExtendedLoadingState<WeeklySummaries?>
    ): List<BittensorSubnetRowUi> {
        return subnets
            .filter { it.matchesQuery(query) }
            .map { subnet -> mapSubnetRow(subnet, favorites, asset, weeklyChange(subnet.id, weeklySummaries)) }
    }

    // A failed or unpriced read and a listed subnet without a change show a dash, only an unlisted one has no history.
    private fun weeklyChange(subnetId: BittensorNetuid.Subnet, state: ExtendedLoadingState<WeeklySummaries?>): BittensorSubnetWeeklyChange {
        val summaries = when (state) {
            ExtendedLoadingState.Loading -> return BittensorSubnetWeeklyChange.Loading
            is ExtendedLoadingState.Error -> return BittensorSubnetWeeklyChange.Unavailable
            is ExtendedLoadingState.Loaded -> state.data ?: return BittensorSubnetWeeklyChange.Unavailable
        }
        if (subnetId !in summaries) return BittensorSubnetWeeklyChange.NotListed
        return summaries[subnetId]?.let(BittensorSubnetWeeklyChange::Available) ?: BittensorSubnetWeeklyChange.Unavailable
    }

    private fun BittensorSubnet.matchesQuery(query: String): Boolean = query.isBlank() ||
        name.contains(query, ignoreCase = true) ||
        symbol.contains(query, ignoreCase = true) ||
        id.value.toString() == query

    private fun BittensorSubnetSorting.analyticsSort() = when (this) {
        BittensorSubnetSorting.POOL_DEPTH -> SubnetSortKey.POOL_SIZE
        BittensorSubnetSorting.AGE -> SubnetSortKey.AGE
        BittensorSubnetSorting.APY -> SubnetSortKey.APY
        BittensorSubnetSorting.SEVEN_DAY_CHANGE -> SubnetSortKey.SEVEN_DAY_CHANGE
        BittensorSubnetSorting.NAME -> SubnetSortKey.NAME
    }

    private fun sortingDisplay(sorting: BittensorSubnetSorting): String = resources.getString(
        when (sorting) {
            BittensorSubnetSorting.SEVEN_DAY_CHANGE -> R.string.bittensor_sort_week
            BittensorSubnetSorting.POOL_DEPTH -> R.string.bittensor_sort_pool
            BittensorSubnetSorting.AGE -> R.string.bittensor_sort_age
            BittensorSubnetSorting.NAME -> R.string.common_name
            BittensorSubnetSorting.APY -> R.string.bittensor_sort_apy
        }
    )

    private fun sortCatalogue(previous: List<BittensorSubnet>, input: BrowseInput): List<BittensorSubnet> {
        val previousOrder = previous.mapNotNull { subnet -> subnet.registrationBlock?.let { BittensorSubnetReference(subnet.id, it) } }
        return sortBittensorSubnets(
            input.subnets,
            input.sorting,
            input.favorites,
            false,
            input.yields,
            input.weekly,
            previousOrder,
            Locale.getDefault()
        ) { resources.getString(R.string.bittensor_subnet_number, it) }
    }

    private fun mapSubnetRow(
        subnet: BittensorSubnet,
        favorites: BittensorSubnetFavorites,
        asset: Asset,
        weeklyChange: BittensorSubnetWeeklyChange
    ): BittensorSubnetRowUi {
        val price = subnet.priceInRao
            ?.let { tokenFormatter.formatToken(it, asset) }
            ?.toString() ?: "—"
        return BittensorSubnetRowUi(
            subnet = subnet,
            metaId = favorites.metaId,
            favorite = subnet in favorites,
            price = price,
            weeklyChange = weeklyChange
        )
    }
}

data class BittensorSubnetRowUi(
    val subnet: BittensorSubnet,
    val metaId: Long,
    val favorite: Boolean,
    val price: String,
    val weeklyChange: BittensorSubnetWeeklyChange = BittensorSubnetWeeklyChange.Loading
)

sealed interface BittensorSubnetWeeklyChange {

    data object Loading : BittensorSubnetWeeklyChange

    data object NotListed : BittensorSubnetWeeklyChange

    data object Unavailable : BittensorSubnetWeeklyChange

    data class Available(val summary: BittensorWeeklyPriceSummary) : BittensorSubnetWeeklyChange
}

private typealias WeeklySummaries = Map<BittensorNetuid.Subnet, BittensorWeeklyPriceSummary?>

private data class WeeklySummaryRequest(val nativePriceId: String?, val subnetIds: Set<BittensorNetuid.Subnet>)

private data class BrowseInput(
    val subnets: List<BittensorSubnet>,
    val sorting: BittensorSubnetSorting,
    val favorites: BittensorSubnetFavorites,
    val yields: Map<BittensorNetuid.Subnet, BigDecimal>,
    val weekly: Map<BittensorNetuid.Subnet, BigDecimal>
)

internal enum class BittensorPickerRootAction {
    OPEN, REPLACE_SUBNET_AMOUNT, RETURN
}

internal fun bittensorPickerRootAction(payload: BittensorChooseSubnetPayload): BittensorPickerRootAction = when {
    !payload.replaceAmount -> BittensorPickerRootAction.OPEN
    payload.replaceOrigin == BittensorReplaceOrigin.ROOT_STAKE -> BittensorPickerRootAction.RETURN
    else -> BittensorPickerRootAction.REPLACE_SUBNET_AMOUNT
}
