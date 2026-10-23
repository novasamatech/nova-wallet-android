package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.StakingFlowSource
import io.novafoundation.nova.analytics.StakingPortfolioSource
import kotlinx.coroutines.flow.onEach
import io.novafoundation.nova.analytics.BittensorDataEndpoint
import io.novafoundation.nova.analytics.BittensorScreen
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorReadAnalytics
import io.novafoundation.nova.analytics.AnalyticsService

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolio
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioChartHistory
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioChartInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioHistoryRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPeriod
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioRowInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioValueSeries
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootClaimInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorChartAxisFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorLoadingValue
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.displayName
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position.BittensorSubnetPositionPayload
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartSelection
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters.RealDateChartTextInjector
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class BittensorDetailsViewModel(
    private val analytics: AnalyticsService,
    private val bittensorPortfolioInteractor: BittensorPortfolioInteractor,
    private val router: StakingRouter,
    private val resources: ResourceManager,
    private val amountFormatter: BittensorAmountFormatter,
    private val rowMapper: BittensorPortfolioRowMapper,
    private val externalActions: ExternalActions.Presentation,
    private val bittensorPortfolioChartInteractor: BittensorPortfolioChartInteractor,
    private val retryController: BittensorRetryController,
    private val chartAxisFormatterFactory: BittensorChartAxisFormatter.Factory,
    private val rowInteractor: BittensorPortfolioRowInteractor,
    private val rootClaimInteractor: BittensorRootClaimInteractor,
    private val analyticsSource: StakingPortfolioSource = StakingPortfolioSource.DASHBOARD,
    private val entry: BittensorPortfolioEntry = BittensorPortfolioEntry.PORTFOLIO,
) : BaseViewModel(), ExternalActions by externalActions {

    private var entryConsumed = false

    private var isOpenedTracked = false

    private val chartSelection = MutableStateFlow<PriceChartSelection?>(null)

    val chartDateFormatter = RealDateChartTextInjector(resources)

    private val chainRpcReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.CHAIN_RPC, BittensorScreen.PORTFOLIO)
    private val subnetsReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.SUBNETS, BittensorScreen.PORTFOLIO)
    private val claimPreviewsReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.CLAIM_PREVIEWS, BittensorScreen.PORTFOLIO)
    private val rootOverviewReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.ROOT_OVERVIEW, BittensorScreen.PORTFOLIO)
    private val priceServerReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.PRICE_SERVER, BittensorScreen.PORTFOLIO)
    private val portfolioHistoryReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.PORTFOLIO_HISTORY, BittensorScreen.PORTFOLIO)

    val retryEvents = retryController.events

    private val portfolioState = retryController.observe(
        step = "portfolio",
        analytics = chainRpcReads,
        transform = { it.withSafeLoading() },
        source = bittensorPortfolioInteractor::observePortfolio
    )
        .shareInBackground()

    private val portfolio = portfolioState.filterLoaded()
        .onEach { data ->
            val positions = data.positions
            if (!isOpenedTracked && positions != null && data.dataStatus !is BittensorDataStatus.Loading) {
                val hasRoot = positions.rootStake.signum() > 0
                val count = positions.subnets.filter { it.alphaInPlanks.signum() > 0 }.distinctBy { it.subnetId }.size +
                    if (hasRoot) 1 else 0
                isOpenedTracked = true
                analytics.track(BittensorAnalyticsEvent.PortfolioOpened(analyticsSource, count, hasRoot, null))
            }
        }
        .shareInBackground()

    val isLoading = portfolioState.map { it.isLoading() || it.dataOrNull?.dataStatus is BittensorDataStatus.Loading }
        .shareInBackground()

    private val positionWarning = portfolioState.map { state ->
        val portfolio = state.dataOrNull
        when {
            portfolio == null || portfolio.dataStatus is BittensorDataStatus.Loading -> null
            portfolio.dataStatus is BittensorDataStatus.Stale -> resources.getString(R.string.bittensor_positions_stale)
            // Missing subnet prices are explained by the rates banner in the header card (iOS).
            else -> null
        }
    }
        .shareInBackground()

    val canAddStake = portfolioState.map { portfolio ->
        val data = portfolio.dataOrNull
        data?.supportsImmediateOperations == true && data.dataStatus is BittensorDataStatus.Fresh
    }
        .onStart { emit(false) }
        .shareInBackground()

    private val chartPeriod = MutableStateFlow(BittensorPortfolioPeriod.THIRTY_DAYS)

    val selectedChartIndex = chartPeriod.map { BittensorPortfolioPeriod.entries.indexOf(it) }
        .shareInBackground()

    private val chartAssetState = retryController.observe(
        step = "chart asset",
        transform = { it.withSafeLoading() },
        source = bittensorPortfolioInteractor::observeAsset
    )
        .shareInBackground()

    private val chartAsset = chartAssetState.filterLoaded()

    private val periods = BittensorPortfolioPeriod.entries

    private val chartButtonTexts = listOf(
        R.string.price_chart_day,
        R.string.bittensor_chart_week,
        R.string.bittensor_chart_month,
        R.string.bittensor_chart_three_months
    )
        .map(resources::getString)

    val chartAxisFormatter = chartAsset.map { asset ->
        chartAxisFormatterFactory.create(asset.token, isFiatMode = true)
    }
        .shareInBackground()

    private val historyRequest = combine(portfolioState, chartPeriod, ::createHistoryRequest)
        .distinctUntilChanged()
        .shareInBackground()

    private val historyState = historyRequest.flatMapLatest { request ->
        if (request == null) {
            flowOf(ExtendedLoadingState.Loaded<BittensorPortfolioChartHistory?>(null))
        } else {
            observePortfolioHistory(request)
        }
    }
        .shareInBackground()

    private val chartData = combine(portfolioState, historyState, chartPeriod, ::mapPortfolioChart)
        .onStart { emit(BittensorPortfolioChart(BittensorPortfolioPeriod.THIRTY_DAYS, ExtendedLoadingState.Loading)) }
        .shareInBackground()

    val warning = combine(positionWarning, historyState, historyRequest) { positions, history, request ->
        val current = history.dataOrNull?.takeIf { it.request == request }
        positions ?: if (current?.history?.account?.status?.stale == true) resources.getString(R.string.bittensor_portfolio_history_stale) else null
    }
        .shareInBackground()

    val totalStake = combine(
        portfolio,
        chartData,
        chartSelection
    ) { data, chart, selection ->
        val sameContext = chart.request?.let { it.metaId == data.metaId && it.chainId == data.chain.id && it.currency == data.asset.token.currency } == true
        val selectedPoint = if (sameContext) chart.selectedPoint(selection) else null
        if (selectedPoint == null) {
            formatAmount(data.positions?.totalStake, data)
        } else {
            val token = data.asset.token
            amountFormatter.amount(selectedPoint.tao, token.configuration.symbol, selectedPoint.fiat, token.currency)
        }
    }
        .shareInBackground()

    val chartChangeFormatter = combine(chartData, chartSelection) { data, selection ->
        BittensorPortfolioChangeTextInjector(data.change(selection), chartButtonTexts[periods.indexOf(data.range)])
    }
        .shareInBackground()

    val priceChartModels = chartData.map { data ->
        periods.mapIndexed { index, period -> mapPriceChart(data, period, chartButtonTexts[index]) }
    }
        .shareInBackground()

    private val subnetMetadata = retryController.observe(
        step = "subnet metadata",
        analytics = subnetsReads,
        transform = { it.withSafeLoading() }
    ) {
        flowOf { bittensorPortfolioInteractor.subnets() }
            .map { subnets -> subnets.associateBy { it.id } }
    }
        .shareInBackground()

    private val rootClaimStates = portfolio.map { it.metaId }.distinctUntilChanged().flatMapLatest { metaId ->
        retryController.observe(
            step = "portfolio root rewards",
            analytics = claimPreviewsReads,
            transform = { it.withSafeLoading() }
        ) {
            rootClaimInteractor.observe(metaId)
        }.map { AccountRowValue(metaId, it) }
    }
        .shareInBackground()

    private val rootRewards = rootClaimStates.map { claims ->
        AccountRowValue(claims.metaId, claims.state.map { it.claims.totalClaimable })
    }
        .shareInBackground()

    val entryResolution = if (entry == BittensorPortfolioEntry.DEEP_LINK) {
        combine(portfolioState, rootClaimStates.onStart { emit(AccountRowValue(null, ExtendedLoadingState.Loading)) }) { state, claims ->
            resolveBittensorEntry(state.dataOrNull, claims.state.dataOrNull)
        }
    } else {
        flowOf(null)
    }
        .shareInBackground()

    private val rootApy = portfolio.map { RootApyRequest(it.metaId, it.chain, it.positions?.root.orEmpty()) }
        .distinctUntilChanged()
        .flatMapLatest { request ->
            retryController.observe(
                step = "portfolio root APY",
                analytics = rootOverviewReads,
                transform = { it.withSafeLoading() }
            ) {
                flowOf { rowInteractor.rootApy(request.chain, request.positions) }
            }.map { AccountRowValue(request.metaId, it) }
        }
        .shareInBackground()

    private val weeklyChanges = portfolioState.map { state ->
        val data = state.dataOrNull
        val priceId = data?.asset?.token?.configuration?.priceId
        if (data?.positions == null || priceId == null) {
            null
        } else {
            WeeklyChangesRequest(data.metaId, priceId, data.positions.subnets.map { it.subnetId }.toSet())
        }
    }
        .distinctUntilChanged()
        .flatMapLatest { request ->
            if (request == null) {
                flowOf(AccountRowValue<Map<BittensorNetuid.Subnet, BigDecimal>>(null, ExtendedLoadingState.Loaded(emptyMap())))
            } else {
                retryController.observe(
                    step = "portfolio weekly changes",
                    analytics = priceServerReads,
                    transform = { it.withSafeLoading() }
                ) {
                    flowOf { rowInteractor.weeklyChanges(request.nativePriceId, request.subnetIds) }
                }.map { AccountRowValue(request.metaId, it) }
            }
        }
        .shareInBackground()

    val rates = combine(portfolio, subnetMetadata, rootApy) { portfolio, metadata, apy ->
        bittensorPortfolioRates(portfolio.positions, metadata, apy.forAccount(portfolio.metaId))
    }
        .distinctUntilChanged()
        .onStart { emit(BittensorPortfolioRates.AVAILABLE) }
        .shareInBackground()

    val empty = combine(portfolio, rootRewards) { portfolio, rewards ->
        isPortfolioEmpty(portfolio.positions, rewards.forAccount(portfolio.metaId).dataOrNull)
    }
        .shareInBackground()

    val positions = combine(portfolio, subnetMetadata, rootRewards, rootApy, weeklyChanges) { portfolio, metadata, rewards, apy, changes ->
        rowMapper.map(
            portfolio,
            metadata,
            rewards.forAccount(portfolio.metaId).dataOrNull,
            apy.forAccount(portfolio.metaId),
            changes.forAccount(portfolio.metaId)
        )
    }
        .shareInBackground()

    private fun createHistoryRequest(state: ExtendedLoadingState<BittensorPortfolio>, period: BittensorPortfolioPeriod): BittensorPortfolioHistoryRequest? {
        val portfolio = state.dataOrNull ?: return null
        val token = portfolio.asset.token
        return BittensorPortfolioHistoryRequest(portfolio.metaId, portfolio.chain.id, token.configuration.priceId, token.currency, period)
    }

    private fun mapPortfolioChart(
        portfolioState: ExtendedLoadingState<BittensorPortfolio>,
        historyState: ExtendedLoadingState<BittensorPortfolioChartHistory?>,
        period: BittensorPortfolioPeriod
    ): BittensorPortfolioChart {
        val portfolio = portfolioState.dataOrNull
        val history = historyState.dataOrNull
        val request = createHistoryRequest(portfolioState, period)
        if (history != null && request != null && history.request != request) {
            return BittensorPortfolioChart(period, ExtendedLoadingState.Loading)
        }
        val state = bittensorLoadingValue(portfolioState, historyState) {
            if (portfolio == null || history == null) {
                BittensorPortfolioValueSeries(emptyList(), null)
            } else {
                bittensorPortfolioChartInteractor.calculate(portfolio, history.history)
            }
        }
        return BittensorPortfolioChart(period, state, request)
    }

    private fun observePortfolioHistory(request: BittensorPortfolioHistoryRequest): Flow<ExtendedLoadingState<BittensorPortfolioChartHistory?>> {
        return retryController.observe(
            step = "portfolio account history",
            analytics = portfolioHistoryReads,
            transform = { it.withSafeLoading() }
        ) {
            bittensorPortfolioChartInteractor.observeHistory(request)
        }
    }

    private fun mapPriceChart(data: BittensorPortfolioChart, period: BittensorPortfolioPeriod, button: String): PriceChartModel {
        if (period != data.range) return PriceChartModel.Loading(button)

        return when (val state = data.chart) {
            is ExtendedLoadingState.Loading -> PriceChartModel.Loading(button)
            is ExtendedLoadingState.Error -> PriceChartModel.Empty(button, resources.getString(R.string.price_chart_load_failed))
            is ExtendedLoadingState.Loaded -> {
                if (state.data.points.isEmpty()) return PriceChartModel.Empty(button, resources.getString(R.string.price_chart_no_data))

                PriceChartModel.Chart(
                    button,
                    button,
                    period != BittensorPortfolioPeriod.NINETY_DAYS,
                    data.prices
                )
            }
        }
    }

    fun selectChartPoint(selection: PriceChartSelection?) {
        chartSelection.value = selection
    }

    fun selectChartPeriod(index: Int) {
        chartSelection.value = null
        periods.getOrNull(index)
            ?.let { chartPeriod.value = it }
    }

    fun back() = router.back()

    fun completeEntry(resolution: BittensorEntryResolution) {
        if (entryConsumed || entry != BittensorPortfolioEntry.DEEP_LINK) return
        entryConsumed = true
        if (resolution is BittensorEntryResolution.Start) {
            analytics.track(AnalyticsEvent.StakingFlowOpened(resolution.network, StakingFlowSource.DEEP_LINK.value))
            router.replaceBittensorPortfolioWithStart(resolution.payload)
        }
    }

    /** Re-entry goes through the same Earn landing as a first stake (iOS `SubtensorPortfolioWireframe.showAddPosition`). */
    fun addPosition() = launchUnit {
        val portfolio = portfolio.first()
        analytics.track(AnalyticsEvent.StakingFlowOpened(portfolio.chain.name, StakingFlowSource.PORTFOLIO_ADD_POSITION.value))
        router.openBittensorStartStaking(portfolio.startStakingPayload())
    }

    fun positionClicked(position: BittensorPositionUi) {
        when (val action = position.action) {
            BittensorPositionAction.RootDetails -> router.openBittensorRootDetails()
            is BittensorPositionAction.ValidatorDetails -> validatorClicked(action.address)
            is BittensorPositionAction.SubnetDetails -> router.openBittensorSubnetPosition(
                BittensorSubnetPositionPayload(action.subnetId.value)
            )
        }
    }

    fun validatorClicked(address: String) = launchUnit {
        externalActions.showAddressActions(
            address,
            portfolio.first().chain
        )
    }

    private fun formatAmount(amount: BigInteger?, portfolio: BittensorPortfolio): AmountModel {
        if (amount == null) return AmountModel("—", null)
        val token = portfolio.asset.token
        val decimalAmount = token.amountFromPlanks(amount)
        return amountFormatter.amount(
            decimalAmount,
            token.configuration.symbol,
            token.toFiatOrNull(decimalAmount),
            token.currency
        )
    }
}

private data class RootApyRequest(val metaId: Long, val chain: Chain, val positions: List<BittensorRootPosition>)

private data class WeeklyChangesRequest(val metaId: Long, val nativePriceId: String, val subnetIds: Set<BittensorNetuid.Subnet>)

private data class AccountRowValue<T>(val metaId: Long?, val state: ExtendedLoadingState<T>) {
    fun forAccount(metaId: Long): ExtendedLoadingState<T> = if (this.metaId == null || this.metaId == metaId) state else ExtendedLoadingState.Loading
}

internal fun subnetPositionTitle(subnet: BittensorSubnet?, fallback: String): String {
    if (subnet == null) return fallback
    return subnet.displayName(fallback)
}

/**
 * iOS `SubtensorPortfolioViewModelFactory`: balances stay live while rates are missing. Missing subnet rates hide the chart
 * (it is valued with them); a missing root rate only drops the APY subtitle.
 */
enum class BittensorPortfolioRates(val showBanner: Boolean, val showChart: Boolean) {
    AVAILABLE(showBanner = false, showChart = true),
    ROOT_UNAVAILABLE(showBanner = true, showChart = true),
    SUBNETS_UNAVAILABLE(showBanner = true, showChart = false)
}

internal fun bittensorPortfolioRates(
    positions: BittensorPortfolioPositions?,
    metadata: ExtendedLoadingState<*>,
    rootApy: ExtendedLoadingState<BigDecimal?>
): BittensorPortfolioRates {
    if (positions == null) return BittensorPortfolioRates.AVAILABLE
    val hasSubnets = positions.subnets.any { it.alphaInPlanks.signum() > 0 }
    val subnetsUnavailable = hasSubnets && (metadata is ExtendedLoadingState.Error || positions.totalStake == null)
    val rootUnavailable = positions.rootStake.signum() > 0 &&
        (rootApy is ExtendedLoadingState.Error || rootApy is ExtendedLoadingState.Loaded && rootApy.data == null)
    return when {
        subnetsUnavailable -> BittensorPortfolioRates.SUBNETS_UNAVAILABLE
        rootUnavailable -> BittensorPortfolioRates.ROOT_UNAVAILABLE
        else -> BittensorPortfolioRates.AVAILABLE
    }
}
