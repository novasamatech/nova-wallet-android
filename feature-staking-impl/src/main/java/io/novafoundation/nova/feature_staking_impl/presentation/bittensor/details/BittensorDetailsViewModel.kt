package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.asUrlIcon
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetRepository
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolio
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPriceChartInteractor
import io.novafoundation.nova.feature_wallet_api.data.repository.PricePeriod
import io.novafoundation.nova.feature_wallet_api.domain.price.AssetPriceChart
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartTextInjectors
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters.RealDateChartTextInjector
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters.RealPricePriceTextInjector
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetDetailsPayload
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorChartAxisFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.ext.addressOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import java.math.BigInteger

data class BittensorPositionUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: AmountModel,
    val icon: Icon,
    val action: BittensorPositionAction,
    val iconSizeDp: Int = 40
)

sealed interface BittensorPositionAction {
    data object RootDetails : BittensorPositionAction
    data class ValidatorDetails(val address: String) : BittensorPositionAction
    data class SubnetDetails(val subnetId: BittensorSubnetId) : BittensorPositionAction
    data object None : BittensorPositionAction
}

class BittensorDetailsViewModel(
    private val bittensorPortfolioInteractor: BittensorPortfolioInteractor,
    private val router: StakingRouter,
    private val resources: ResourceManager,
    private val amountFormatter: AmountFormatter,
    private val assetIconProvider: AssetIconProvider,
    private val iconGenerator: AddressIconGenerator,
    private val externalActions: ExternalActions.Presentation,
    private val bittensorPriceChartInteractor: BittensorPriceChartInteractor,
    private val logger: Logger,
    private val subnetRepository: BittensorSubnetRepository,
    private val chartAxisFormatterFactory: BittensorChartAxisFormatter.Factory
) : BaseViewModel(), ExternalActions by externalActions {

    private val reload = MutableStateFlow(0L)

    private val portfolioState = reload.flatMapLatest {
        bittensorPortfolioInteractor.observePortfolio()
            .map<BittensorPortfolio, ExtendedLoadingState<BittensorPortfolio>> { ExtendedLoadingState.Loaded(it) }
            .onStart { emit(ExtendedLoadingState.Loading) }
            .catch {
                logger.log("[BittensorPortfolio] Portfolio stream failed")
                logger.log(it)
                emit(ExtendedLoadingState.Error(it))
            }
    }.shareInBackground()

    private val portfolio = portfolioState.filterLoaded().shareInBackground()

    private val failed = portfolioState.map { it is ExtendedLoadingState.Error }.shareInBackground()

    val isLoading = combine(portfolio.map { it.dataStatus is BittensorDataStatus.Loading }.onStart { emit(true) }, failed) { loading, failed ->
        loading && !failed
    }
        .shareInBackground()

    private val portfolioWarning = portfolio.map {
        when {
            it.dataStatus is BittensorDataStatus.Loading -> null
            it.positions == null -> resources.getString(R.string.common_retry_message)
            it.dataStatus is BittensorDataStatus.Stale -> resources.getString(R.string.bittensor_positions_stale)
            it.positions.totalStake == null -> resources.getString(R.string.bittensor_valuation_unavailable)
            else -> null
        }
    }.onStart { emit(null) }

    val warning = combine(portfolioWarning, failed) { warning, failed ->
        if (failed) resources.getString(R.string.common_retry_message) else warning
    }.shareInBackground()

    val totalStake = portfolio.map { data -> format(data.positions?.totalStake, data) }.shareInBackground()

    val rootStake = portfolio.map { data -> format(data.positions?.rootStake, data) }.shareInBackground()

    val hasRootStake = portfolio.map { it.positions?.rootStake?.signum() == 1 }.shareInBackground()

    val assetIcon = portfolio.map { assetIconProvider.getAssetIconOrFallback(it.asset.token.configuration) }.shareInBackground()

    val canAddStake = combine(portfolio, failed) { data, failed -> !failed && data.dataStatus is BittensorDataStatus.Fresh }
        .onStart { emit(false) }.shareInBackground()

    val empty = portfolio.map { it.positions?.let { p -> p.root.isEmpty() && p.subnets.isEmpty() } == true }.shareInBackground()

    private val chartPeriod = MutableStateFlow(PricePeriod.MONTH)

    val selectedChartIndex = chartPeriod.map { PricePeriod.entries.indexOf(it) }.shareInBackground()

    private val chartReload = MutableStateFlow(0L)

    private val chartAssetState = chartReload.flatMapLatest {
        bittensorPortfolioInteractor.observeAsset()
            .map<Asset, ExtendedLoadingState<Asset>> { ExtendedLoadingState.Loaded(it) }
            .onStart { emit(ExtendedLoadingState.Loading) }
            .catch {
                logger.log("[BittensorChart] Failed to load native asset")
                logger.log(it)
                emit(ExtendedLoadingState.Error(it))
            }
    }.shareInBackground()

    private val chartAsset = chartAssetState.filterLoaded()

    private val periods = PricePeriod.entries

    private val chartButtonTexts = listOf(
        R.string.price_chart_day,
        R.string.bittensor_chart_week,
        R.string.bittensor_chart_month,
        R.string.price_chart_year,
        R.string.common_all
    ).map(resources::getString)

    val priceChartTitle = chartAsset.map { resources.getString(R.string.price_chart_title, it.token.configuration.symbol.value) }
        .shareInBackground()

    val priceChartDisclaimer = chartAsset.map {
        resources.getString(R.string.bittensor_chart_price_disclaimer, it.token.configuration.symbol.value)
    }.shareInBackground()

    val chartAxisFormatter = chartAsset.map { asset ->
        chartAxisFormatterFactory.create(asset.token, isFiatMode = true)
    }.shareInBackground()

    val priceChartFormatters = combine(chartAsset, chartPeriod) { asset, period ->
        val token = asset.token
        PriceChartTextInjectors(
            RealPricePriceTextInjector(token.currency, token.coinRate?.rate),
            BittensorChartChangeTextInjector(chartButtonTexts[periods.indexOf(period)]),
            RealDateChartTextInjector(resources)
        )
    }.shareInBackground()

    private val chartData = combine(
        chartAssetState.mapLoading { it.token.configuration.priceId to it.token.currency }.distinctUntilChanged(),
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
                        bittensorPriceChartInteractor.observe(priceId, currency, period)
                    }
                }
            }
        }.onStart { emit(AssetPriceChart(PricePeriod.MONTH, ExtendedLoadingState.Loading)) }.shareInBackground()

    val chartFailed = chartData.map { it.chart is ExtendedLoadingState.Error }.shareInBackground()

    val priceChartModels = chartData.map { data ->
        periods.mapIndexed { index, period ->
            val button = chartButtonTexts[index]
            if (period != data.range) return@mapIndexed PriceChartModel.Loading(button)
            when (val state = data.chart) {
                is ExtendedLoadingState.Loading -> PriceChartModel.Loading(button)
                is ExtendedLoadingState.Error -> PriceChartModel.Empty(button, resources.getString(R.string.price_chart_load_failed))
                is ExtendedLoadingState.Loaded -> if (state.data.isEmpty()) {
                    PriceChartModel.Empty(button, resources.getString(R.string.price_chart_no_data))
                } else {
                    PriceChartModel.Chart(
                        button,
                        button,
                        period != PricePeriod.YEAR && period != PricePeriod.MAX,
                        state.data.map { PriceChartModel.Chart.Price(it.timestamp, it.rate) }
                    )
                }
            }
        }
    }.shareInBackground()

    fun selectChartPeriod(index: Int) {
        periods.getOrNull(index)?.let { chartPeriod.value = it }
    }

    fun retryChart() {
        chartReload.update { it + 1 }
    }

    private val subnetMetadata = reload.flatMapLatest {
        flow {
            emit(emptyMap<BittensorSubnetId, BittensorSubnet>())
            try {
                emit(subnetRepository.subnets().associateBy { it.id })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.log("[BittensorPortfolio] Failed to load subnet metadata")
                logger.log(e)
            }
        }
    }.shareInBackground()

    val positions = combine(portfolio, subnetMetadata) { data, metadata ->
        val positions = data.positions ?: return@combine emptyList()
        buildList {
            if (positions.rootStake.signum() > 0) {
                add(
                    BittensorPositionUi(
                        "root",
                        resources.getString(R.string.bittensor_root_staking),
                        resources.getString(R.string.bittensor_apy_unavailable),
                        format(positions.rootStake, data),
                        assetIconProvider.getAssetIconOrFallback(data.asset.token.configuration),
                        BittensorPositionAction.RootDetails
                    )
                )
            }
            positions.subnets.map { it.subnetId }.distinct().sortedBy { it.value }.forEach { subnetId ->
                val subnet = metadata[subnetId]
                val fallback = resources.getString(R.string.bittensor_subnet_number, subnetId.value)
                add(
                    BittensorPositionUi(
                        "subnet-${subnetId.value}",
                        subnetPositionTitle(subnet, fallback),
                        if (subnet == null) resources.getString(R.string.bittensor_subnet_metadata_unavailable) else fallback,
                        format(positions.subnetStake(subnetId), data),
                        subnet?.logoUrl?.asUrlIcon() ?: Icon.FromDrawableRes(R.drawable.ic_fallback_network_icon),
                        BittensorPositionAction.SubnetDetails(subnetId)
                    )
                )
            }
        }
    }.shareInBackground()

    val validators = portfolio.map { data ->
        data.positions?.root.orEmpty().map { position ->
            val address = data.chain.addressOf(position.validatorHotkey.value)
            BittensorPositionUi(
                id = address,
                title = address.take(6) + "…" + address.takeLast(6),
                subtitle = resources.getString(R.string.bittensor_validator),
                amount = AmountModel("—", resources.getString(R.string.bittensor_validator_apr_period)),
                icon = Icon.FromDrawable(iconGenerator.createAddressIcon(position.validatorHotkey.value, 32)),
                action = BittensorPositionAction.ValidatorDetails(address),
                iconSizeDp = 32
            )
        }
    }.shareInBackground()

    fun back() = router.back()

    fun addStake() = router.openBittensorRootStaking()

    fun addPosition() = router.openBittensorChooseSubnet()

    fun retry() {
        reload.update { it + 1 }
    }

    fun positionClicked(position: BittensorPositionUi) {
        when (val action = position.action) {
            BittensorPositionAction.RootDetails -> router.openBittensorRootDetails()
            is BittensorPositionAction.ValidatorDetails -> validatorClicked(action.address)
            is BittensorPositionAction.SubnetDetails -> router.openBittensorSubnetDetails(BittensorSubnetDetailsPayload(action.subnetId.value))
            BittensorPositionAction.None -> Unit
        }
    }

    fun validatorClicked(address: String) = launchUnit {
        externalActions.showAddressActions(address, portfolio.first().chain)
    }

    private fun format(amount: BigInteger?, portfolio: BittensorPortfolio): AmountModel {
        if (amount == null) return AmountModel("—", null)
        val token = portfolio.asset.token
        val model = amountFormatter.formatAmountToAmountModel(amount, token)
        return model.copy(fiat = model.fiat.takeIf { token.toFiatOrNull(token.amountFromPlanks(amount)) != null })
    }
}

internal fun subnetPositionTitle(subnet: BittensorSubnet?, fallback: String): String {
    if (subnet == null) return fallback
    return listOf(subnet.name.ifBlank { fallback }, subnet.symbol).filter(String::isNotBlank).joinToString(" ")
}
