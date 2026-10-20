package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.domain.isError
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.domain.map
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

    private val weeklySummariesState = combine(catalogue, asset) { catalogue, asset ->
        WeeklySummaryRequest(asset.token.configuration.priceId, catalogue.map { it.id }.toSet())
    }
        .distinctUntilChanged()
        .flatMapLatest { request ->
            retryController.observe(
                step = "subnet weekly changes",
                transform = { it.withSafeLoading() }
            ) {
                flowOf {
                    val priceId = request.nativePriceId
                    if (priceId == null) emptyMap() else rowInteractor.weeklySummaries(priceId, request.subnetIds)
                }
            }
        }
        .shareInBackground()

    private val weekly = weeklySummariesState.map { state -> state.dataOrNull.orEmpty().mapValues { it.value.changePercent } }
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

    val listState = combine(rootIcon, subnets, isLoading) { icon, subnets, loading ->
        BittensorSubnetListState(
            items = bittensorSubnetList(icon, if (loading) emptyList() else subnets),
            loading = loading
        )
    }
        .distinctUntilChanged()
        .shareInBackground()

    val subnetCount = combine(filteredCatalogue, catalogue, query) { filtered, all, query ->
        resources.getString(R.string.bittensor_subnet_count, filtered.count { it.matchesQuery(query) }, all.size)
    }
        .shareInBackground()

    val isEmpty = combine(subnets, isLoading, hasError) { subnets, loading, failed -> subnets.isEmpty() && !loading && !failed }
        .shareInBackground()

    val emptyMessage = query.map { query ->
        if (query.isEmpty()) {
            resources.getString(R.string.bittensor_subnet_filter_empty)
        } else {
            resources.getString(R.string.bittensor_subnet_search_empty, query)
        }
    }
        .shareInBackground()

    init {
        validatorCommunicator.responseFlow
            .onEach { selectionState = selectionState.withResponse(it) }
            .launchIn(this)
    }

    fun search(query: String) {
        this.query.value = query.trim()
    }

    fun select(row: BittensorSubnetRowUi) = launchUnit {
        rememberBrowseState()
        router.openBittensorSubnetDetails(
            BittensorSubnetDetailsPayload(row.subnet.id.value, payload.replaceAmount, selectionState = selectionState)
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showError(e)
        }
    }

    fun rootStaking() = launchUnit {
        rememberBrowseState()
        if (payload.replaceAmount) {
            router.replaceBittensorSubnetAmountWithRoot(selectionState)
        } else {
            router.openBittensorRootStaking(selectionState = selectionState)
        }
    }

    fun back() = launchUnit {
        rememberBrowseState()
        validatorCommunicator.respond(BittensorSelectValidatorCommunicator.Response(null, null, selectionState))
        router.back()
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
        weeklySummaries: ExtendedLoadingState<Map<BittensorNetuid.Subnet, BittensorWeeklyPriceSummary>>
    ): List<BittensorSubnetRowUi> {
        return subnets
            .filter { it.matchesQuery(query) }
            .map { subnet ->
                mapSubnetRow(subnet, favorites, asset).copy(weeklySummary = weeklySummaries.map { it[subnet.id] })
            }
    }

    private fun BittensorSubnet.matchesQuery(query: String): Boolean = query.isBlank() ||
        name.contains(query, ignoreCase = true) ||
        symbol.contains(query, ignoreCase = true) ||
        id.value.toString() == query

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
        )
    }

    private fun mapSubnetRow(subnet: BittensorSubnet, favorites: BittensorSubnetFavorites, asset: Asset): BittensorSubnetRowUi {
        val price = subnet.priceInRao
            ?.let { tokenFormatter.formatToken(it, asset) }
            ?.toString() ?: "—"
        return BittensorSubnetRowUi(
            subnet = subnet,
            metaId = favorites.metaId,
            favorite = subnet in favorites,
            price = price
        )
    }
}

data class BittensorSubnetRowUi(
    val subnet: BittensorSubnet,
    val metaId: Long,
    val favorite: Boolean,
    val price: String,
    val weeklySummary: ExtendedLoadingState<BittensorWeeklyPriceSummary?> = ExtendedLoadingState.Loaded(null)
)

private data class WeeklySummaryRequest(val nativePriceId: String?, val subnetIds: Set<BittensorNetuid.Subnet>)

private data class BrowseInput(
    val subnets: List<BittensorSubnet>,
    val sorting: BittensorSubnetSorting,
    val favorites: BittensorSubnetFavorites,
    val yields: Map<BittensorNetuid.Subnet, BigDecimal>,
    val weekly: Map<BittensorNetuid.Subnet, BigDecimal>
)
