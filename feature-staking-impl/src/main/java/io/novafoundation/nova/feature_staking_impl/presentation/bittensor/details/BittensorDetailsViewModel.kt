package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.domain.onError
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.asUrlIcon
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicRecoveryState
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicSubmissionRecovery
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolio
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPriceChartInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorChartAxisFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.displayName
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.toUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorRecoveryRequest
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorWalletRecovery
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position.BittensorSubnetPositionPayload
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.domain.price.AssetPriceChart
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartTextInjectors
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters.RealDateChartTextInjector
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters.RealPricePriceTextInjector
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class BittensorDetailsViewModel(
    private val bittensorPortfolioInteractor: BittensorPortfolioInteractor,
    private val router: StakingRouter,
    private val resources: ResourceManager,
    private val amountFormatter: BittensorAmountFormatter,
    private val assetIconProvider: AssetIconProvider,
    private val externalActions: ExternalActions.Presentation,
    private val bittensorPriceChartInteractor: BittensorPriceChartInteractor,
    private val retryController: BittensorRetryController,
    private val chartAxisFormatterFactory: BittensorChartAxisFormatter.Factory,
    private val fiatFormatter: FiatFormatter,
    private val recovery: ExtrinsicSubmissionRecovery,
    private val accountRepository: AccountRepository
) : BaseViewModel(), ExternalActions by externalActions {

    val retryEvents = retryController.events

    private val portfolioState = retryController.observe(
        step = "portfolio",
        transform = { it.withSafeLoading() },
        source = bittensorPortfolioInteractor::observePortfolio
    )
        .shareInBackground()

    private val portfolio = portfolioState.filterLoaded()
        .shareInBackground()

    val isLoading = portfolioState.map { it.isLoading() || it.dataOrNull?.dataStatus is BittensorDataStatus.Loading }
        .shareInBackground()

    private val recoveryState = accountRepository.selectedMetaAccountFlow().map { BittensorRecoveryRequest(it.id, ChainGeneses.BITTENSOR) }
        .distinctUntilChanged().flatMapLatest { (metaId, chainId) ->
            recovery.observe(metaId, chainId).map { BittensorWalletRecovery(metaId, it) }
                .onStart { emit(BittensorWalletRecovery(metaId, ExtrinsicRecoveryState.None)) }
        }
        .onStart { emit(BittensorWalletRecovery(0L, ExtrinsicRecoveryState.None)) }
        .shareInBackground()

    val recoveryResult = recoveryState.map { (metaId, state) ->
        (state as? ExtrinsicRecoveryState.Resolved)
            ?.result?.takeUnless { it.acknowledged }
            ?.toUi(resources, metaId)
    }
        .shareInBackground()

    fun recoveryResultDismissed(metaId: Long, hash: String) = launchUnit {
        recovery.acknowledge(metaId, hash)
    }

    private val portfolioWarning = portfolioState.map { state ->
        val portfolio = state.dataOrNull
        when {
            portfolio == null || portfolio.dataStatus is BittensorDataStatus.Loading -> null
            portfolio.dataStatus is BittensorDataStatus.Stale -> resources.getString(R.string.bittensor_positions_stale)
            portfolio.positions?.totalStake == null -> resources.getString(R.string.bittensor_valuation_unavailable)
            else -> null
        }
    }
        .shareInBackground()

    val totalStake = portfolio.map { data -> formatAmount(data.positions?.totalStake, data) }
        .shareInBackground()

    val warning = combine(
        portfolioWarning,
        recoveryState
    ) { warning, recovery ->
        if (recovery.state is ExtrinsicRecoveryState.Pending) resources.getString(R.string.bittensor_submission_unknown_description) else warning
    }
        .shareInBackground()

    val canAddStake = combine(
        portfolioState,
        recoveryState
    ) { portfolio, recovery ->
        val data = portfolio.dataOrNull
        data?.supportsImmediateOperations == true && data.metaId == recovery.metaId &&
            data.dataStatus is BittensorDataStatus.Fresh && recovery.state !is ExtrinsicRecoveryState.Pending
    }
        .onStart { emit(false) }
        .shareInBackground()

    val empty = portfolio.map { it.positions?.let { p -> p.root.isEmpty() && p.subnets.isEmpty() } == true }
        .shareInBackground()

    private val chartPeriod = MutableStateFlow(PricePeriod.MONTH)

    val selectedChartIndex = chartPeriod.map { PricePeriod.entries.indexOf(it) }
        .shareInBackground()

    private val chartAssetState = retryController.observe(
        step = "chart asset",
        transform = { it.withSafeLoading() },
        source = bittensorPortfolioInteractor::observeAsset
    )
        .shareInBackground()

    private val chartAsset = chartAssetState.filterLoaded()

    private val periods = PricePeriod.entries

    private val chartButtonTexts = listOf(
        R.string.price_chart_day,
        R.string.bittensor_chart_week,
        R.string.bittensor_chart_month,
        R.string.price_chart_year,
        R.string.common_all
    )
        .map(resources::getString)

    val priceChartTitle = chartAsset.map { resources.getString(R.string.price_chart_title, it.token.configuration.symbol.value) }
        .shareInBackground()

    val priceChartDisclaimer = chartAsset.map {
        resources.getString(R.string.bittensor_chart_price_disclaimer, it.token.configuration.symbol.value)
    }
        .shareInBackground()

    val chartAxisFormatter = chartAsset.map { asset ->
        chartAxisFormatterFactory.create(asset.token, isFiatMode = true)
    }
        .shareInBackground()

    val priceChartFormatters = combine(
        chartAsset,
        chartPeriod
    ) { asset, period ->
        val token = asset.token
        val priceTextInjector = RealPricePriceTextInjector(token.currency, token.coinRate?.rate, fiatFormatter)
        val changeTextInjector = BittensorChartChangeTextInjector(chartButtonTexts[periods.indexOf(period)])
        val dateTextInjector = RealDateChartTextInjector(resources)
        PriceChartTextInjectors(priceTextInjector, changeTextInjector, dateTextInjector)
    }
        .shareInBackground()

    private val chartData = combine(
        chartAssetState.mapLoading { it.token.configuration.priceId to it.token.currency }
            .distinctUntilChanged(),
        chartPeriod
    ) { identity, period -> identity to period }
        .flatMapLatest { (identity, period) ->
            when (identity) {
                is ExtendedLoadingState.Loading -> flowOf(AssetPriceChart(period, ExtendedLoadingState.Loading))
                is ExtendedLoadingState.Error -> flowOf(AssetPriceChart(period, identity))
                is ExtendedLoadingState.Loaded -> {
                    val (priceId, currency) = identity.data
                    if (priceId == null) {
                        flowOf(AssetPriceChart(period, ExtendedLoadingState.Loaded(emptyList())))
                    } else {
                        observePriceChart(priceId, currency, period)
                    }
                }
            }
        }
        .onStart { emit(AssetPriceChart(PricePeriod.MONTH, ExtendedLoadingState.Loading)) }
        .shareInBackground()

    val priceChartModels = chartData.map { data ->
        periods.mapIndexed { index, period -> mapPriceChart(data, period, chartButtonTexts[index]) }
    }
        .shareInBackground()

    private fun observePriceChart(priceId: String, currency: Currency, period: PricePeriod): Flow<AssetPriceChart> {
        return retryController.observe(
            step = "price chart",
            transform = { prices ->
                prices.withSafeLoading().map { AssetPriceChart(period, it) }
            }
        ) {
            bittensorPriceChartInteractor.observe(priceId, currency, period).map { it.chart }
                .onError { throw IllegalStateException("Price chart request failed", it) }
                .filterLoaded()
        }
    }

    private fun mapPriceChart(data: AssetPriceChart, period: PricePeriod, button: String): PriceChartModel {
        if (period != data.range) return PriceChartModel.Loading(button)

        return when (val state = data.chart) {
            is ExtendedLoadingState.Loading -> PriceChartModel.Loading(button)
            is ExtendedLoadingState.Error -> PriceChartModel.Empty(button, resources.getString(R.string.price_chart_load_failed))
            is ExtendedLoadingState.Loaded -> {
                if (state.data.isEmpty()) return PriceChartModel.Empty(button, resources.getString(R.string.price_chart_no_data))

                val prices = state.data.map { PriceChartModel.Chart.Price(it.timestamp, it.rate) }
                PriceChartModel.Chart(
                    button,
                    button,
                    period != PricePeriod.YEAR && period != PricePeriod.MAX,
                    prices
                )
            }
        }
    }

    fun selectChartPeriod(index: Int) {
        periods.getOrNull(index)
            ?.let { chartPeriod.value = it }
    }

    private val subnetMetadata = retryController.observe(
        step = "subnet metadata",
        transform = { it.catch { } }
    ) {
        flowOf { bittensorPortfolioInteractor.subnets() }
            .map { subnets -> subnets.associateBy { it.id } }
    }
        .onStart { emit(emptyMap()) }
        .shareInBackground()

    val positions = combine(portfolio, subnetMetadata, ::mapPositions)
        .shareInBackground()

    private fun mapPositions(portfolio: BittensorPortfolio, metadata: Map<BittensorNetuid.Subnet, BittensorSubnet>): List<BittensorPositionUi> {
        val positions = portfolio.positions ?: return emptyList()
        val subnetIds = positions.subnets
            .map { it.subnetId }
            .distinct().sortedBy { it.value }

        return buildList {
            if (positions.rootStake.isPositive()) add(mapRootPosition(portfolio, positions.rootStake))
            subnetIds.forEach { subnetId -> add(mapSubnetPosition(portfolio, subnetId, metadata[subnetId])) }
        }
    }

    private fun mapRootPosition(portfolio: BittensorPortfolio, stake: BigInteger): BittensorPositionUi = BittensorPositionUi(
        "root",
        resources.getString(R.string.bittensor_root_staking),
        resources.getString(R.string.bittensor_apy_unavailable),
        formatAmount(stake, portfolio),
        assetIconProvider.getAssetIconOrFallback(portfolio.asset.token.configuration),
        BittensorPositionAction.RootDetails
    )

    private fun mapSubnetPosition(portfolio: BittensorPortfolio, subnetId: BittensorNetuid.Subnet, subnet: BittensorSubnet?): BittensorPositionUi {
        val fallback = resources.getString(R.string.bittensor_subnet_number, subnetId.value)
        val subtitle = if (subnet == null) resources.getString(R.string.bittensor_subnet_metadata_unavailable) else fallback
        val icon = subnet?.logoUrl?.asUrlIcon() ?: Icon.FromDrawableRes(R.drawable.ic_fallback_network_icon)
        return BittensorPositionUi(
            "subnet-${subnetId.value}",
            subnetPositionTitle(subnet, fallback),
            subtitle,
            formatAmount(portfolio.positions?.subnetStake(subnetId), portfolio),
            icon,
            BittensorPositionAction.SubnetDetails(subnetId)
        )
    }

    fun back() = router.back()

    fun addPosition() = router.openBittensorChooseSubnet()

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

internal fun subnetPositionTitle(subnet: BittensorSubnet?, fallback: String): String {
    if (subnet == null) return fallback
    return subnet.displayName(fallback)
}
