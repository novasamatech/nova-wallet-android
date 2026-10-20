package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position

import io.novasama.substrate_sdk_android.extensions.toHexString

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetChart
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.hasBittensorSubnetHistoryCoverage
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.formatAsPercentage
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorYieldColor
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorEstimatesMixin
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimateContext
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorProtocolUnits
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorWithdrawalRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPositionWithdrawalInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorSlippageSettings
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorWithdrawalFormatter
import kotlinx.coroutines.flow.distinctUntilChanged
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetChartPeriod
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetDetailsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetMetadataInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.labelRes
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorPositionAction
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorPositionUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitPositionPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorSaleCostBasisFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetAmountPayload
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartModel
import io.novafoundation.nova.runtime.ext.addressOf
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartSelection
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters.RealDateChartTextInjector
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorChartChangeTextInjector
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

enum class BittensorPositionTradeAction { BUY, SELL }

data class BittensorPositionChoiceRequest(val action: BittensorPositionTradeAction, val positions: List<BittensorExitPositionPayload>)

class BittensorSubnetPositionViewModel(
    private val subnetId: BittensorNetuid.Subnet,
    private val bittensorSubnetInteractor: BittensorSubnetInteractor,
    private val bittensorSubnetDetailsInteractor: BittensorSubnetDetailsInteractor,
    private val subnetMetadataInteractor: BittensorSubnetMetadataInteractor,
    private val router: StakingRouter,
    private val resources: ResourceManager,
    private val tokenFormatter: TokenFormatter,
    private val addressIconGenerator: AddressIconGenerator,
    private val externalActions: ExternalActions.Presentation,
    private val logger: Logger,
    private val fiatFormatter: FiatFormatter,
    estimatesMixin: BittensorEstimatesMixin,
    private val retryController: BittensorRetryController,
    private val withdrawalInteractor: BittensorPositionWithdrawalInteractor,
    private val withdrawalFormatter: BittensorWithdrawalFormatter,
    slippageSettings: BittensorSlippageSettings,
    private val costBasisFormatter: BittensorSaleCostBasisFormatter
) : BaseViewModel(), ExternalActions by externalActions {

    private val chartSelection = MutableStateFlow<PriceChartSelection?>(null)

    val chartDateFormatter = RealDateChartTextInjector(resources)

    val retryEvents = retryController.events

    private val portfolioState = retryController.observe(
        step = "subnet portfolio",
        transform = { it.withSafeLoading() },
        source = bittensorSubnetInteractor::observePortfolio
    )
        .shareInBackground()

    private val portfolio = portfolioState.map { it.dataOrNull }
        .shareInBackground()

    private val metadataState = retryController.observe(
        step = "subnet metadata",
        transform = { it.withSafeLoading() }
    ) {
        flowOf { subnetMetadataInteractor.subnet(subnetId) }
    }
        .shareInBackground()

    val subnet = metadataState.map { it.dataOrNull }
        .shareInBackground()

    private val held = portfolio.map { data ->
        data?.positions?.subnets.orEmpty().filter { it.subnetId == subnetId && it.alphaInPlanks.isPositive() }
    }
        .shareInBackground()

    val canAct = combine(
        portfolio,
        held,
        metadataState
    ) { data, held, subnetMetadataInteractor ->
        data?.supportsImmediateOperations == true && data.dataStatus is BittensorDataStatus.Fresh &&
            held.isNotEmpty() && subnetMetadataInteractor.dataOrNull != null
    }
        .shareInBackground()

    val hasStake = held.map {
        it.maxByOrNull { it.alphaInPlanks }
            ?.isRegistered == true
    }
        .shareInBackground()

    private val withdrawalState = combine(portfolio, held, slippageSettings.observe()) { data, positions, slippage ->
        val primary = positions.sortedBy { it.validatorHotkey.value.toHexString() }.maxByOrNull { it.alphaInPlanks }
        if (data == null || data.dataStatus !is BittensorDataStatus.Fresh || primary == null) {
            null
        } else {
            BittensorWithdrawalRequest(data.metaId, data.chain.id, subnetId, primary.validatorHotkey, slippage)
        }
    }.distinctUntilChanged().flatMapLatest { request ->
        retryController.observe(
            step = "subnet withdrawal availability",
            transform = { it.withSafeLoading() }
        ) {
            if (request == null) kotlinx.coroutines.flow.flowOf(null) else withdrawalInteractor.observe(request)
        }
    }.shareInBackground()

    val withdrawalLoading = withdrawalState.map { it.isLoading() }.shareInBackground()

    val withdrawalHint = combine(withdrawalState, subnet) { state, subnet ->
        val withdrawal = state.dataOrNull ?: return@combine null
        val symbol = subnet?.symbol?.takeIf { it.isNotBlank() } ?: return@combine null
        withdrawalFormatter.format(withdrawal, BittensorProtocolUnits.ALPHA_PRECISION, TokenSymbol(symbol))
    }.shareInBackground()

    val canSell = combine(canAct, withdrawalState) { allowed, state -> allowed && state.dataOrNull?.canWithdraw == true }
        .shareInBackground()

    private val costBasisUpdate = estimatesMixin.observePositionCostBasis(
        portfolio.map { it?.let { BittensorEstimateContext(it.metaId, it.chain) } }
    )
        .shareInBackground()

    private val costBasisState = costBasisUpdate.map { it.state }.shareInBackground()

    private val yieldState = estimatesMixin.observeAlphaYields(
        portfolio.map { it?.chain },
        subnetId
    )
        .shareInBackground()

    private val chartAssetState = retryController.observe(
        step = "position chart asset",
        transform = { it.withSafeLoading() },
        source = bittensorSubnetInteractor::observeAsset
    )
        .shareInBackground()

    val averageChartPrice = costBasisState.map {
        it.dataOrNull?.forSubnet(subnetId.value)
            ?.averagePrice
    }
        .shareInBackground()

    val price = combine(
        subnet,
        chartAssetState,
        chartSelection
    ) { subnet, assetState, selection ->
        val asset = assetState.dataOrNull
        val price = selection?.point?.price ?: subnet?.priceInRao?.let { asset?.token?.amountFromPlanks(it) }
        if (price == null || asset == null) {
            "—"
        } else {
            tokenFormatter.formatToken(price, asset.token.configuration.symbol)
        }
    }
        .shareInBackground()

    val averagePrice = combine(
        costBasisState,
        chartAssetState,
        subnet
    ) { state, assetState, subnet ->
        state.map { basis ->
            costBasisFormatter.averagePrice(basis, subnetId.value, assetState.dataOrNull?.token, subnet?.symbol)
        }
    }
        .shareInBackground()

    val earned = combine(
        costBasisUpdate,
        portfolio,
        held
    ) { update, data, positions ->
        val value = update.state.map { basis ->
            val totals = basis?.forSubnet(subnetId.value)
            val worth = data?.positions?.subnetStake(subnetId)
            if (totals == null || worth == null) {
                costBasisFormatter.profit(null, null)
            } else {
                val alpha = positions.fold(BigInteger.ZERO) { sum, position -> sum + position.alphaInPlanks }
                costBasisFormatter.profit(
                    data.asset.token.amountFromPlanks(totals.estimatedProfit(alpha, worth)),
                    data.asset.token
                )
            }
        }
        BittensorPositionEarnedUi(value, update.showsEarned(subnetId))
    }
        .shareInBackground()

    val holding = combine(
        portfolio,
        held,
        subnet
    ) { data, positions, subnet ->
        if (data?.positions == null) return@combine "—"
        val amount = positions.fold(BigInteger.ZERO) { sum, position -> sum + position.alphaInPlanks }
        tokenFormatter.formatToken(
            BigDecimal(amount, BittensorProtocolUnits.ALPHA_PRECISION),
            TokenSymbol(
                subnet?.symbol.orEmpty().ifBlank { "—" }
            )
        ).toString()
    }
        .shareInBackground()

    val worth = portfolioState.mapLoading { data ->
        val planks = data.positions?.subnetStake(subnetId)
        if (planks == null) {
            AmountModel("—", null)
        } else {
            val tao = data.asset.token.amountFromPlanks(planks)
            AmountModel(
                tokenFormatter.formatToken(tao, data.asset.token.configuration.symbol).toString(),
                data.asset.token.toFiatOrNull(tao)
                    ?.let { fiatFormatter.formatFiat(it, data.asset.token.currency) }
            )
        }
    }
        .shareInBackground()

    val validators = combine(
        portfolio,
        held,
        yieldState
    ) { data, held, yields ->
        if (data == null) {
            emptyList()
        } else {
            held.map { position ->
                val address = data.chain.addressOf(position.validatorHotkey.value)
                val annualYield = yields.dataOrNull?.get(position.validatorHotkey)
                BittensorPositionUi(
                    address,
                    address.take(6) + "…" + address.takeLast(6),
                    resources.getString(R.string.bittensor_validator),
                    AmountModel(
                        annualYield?.formatAsPercentage() ?: "",
                        resources.getString(R.string.bittensor_estimated_apy)
                    ),
                    Icon.FromDrawable(addressIconGenerator.createAddressIcon(position.validatorHotkey.value, 32)),
                    BittensorPositionAction.ValidatorDetails(address),
                    32,
                    isAmountLoading = yields is ExtendedLoadingState.Loading,
                    amountColorRes = bittensorYieldColor(annualYield)
                )
            }
        }
    }
        .shareInBackground()

    private val period = MutableStateFlow(BittensorSubnetChartPeriod.WEEK)

    private data class ChartRequest(val asset: Asset?, val period: BittensorSubnetChartPeriod)

    private val chartState = combine(
        chartAssetState,
        period
    ) { asset, period ->
        ChartRequest(asset.dataOrNull, period)
    }
        .flatMapLatest { request ->
            retryController.observe(
                step = "position price chart",
                transform = { it.withSafeLoading() }
            ) { loadChart(request) }
                .map { BittensorSubnetChart(request.period, it) }
        }
        .shareInBackground()

    val priceChangeFormatter = combine(chartState, period) { chart, selected ->
        val covered = chart.period == selected && hasBittensorSubnetHistoryCoverage(chart.points.dataOrNull.orEmpty(), selected)
        BittensorChartChangeTextInjector(resources.getString(selected.labelRes()), covered)
    }
        .shareInBackground()

    val axisFormatter = chartAssetState
        .mapNotNull { it.dataOrNull }
        .map { BittensorPositionChartFormatter(tokenFormatter, it.token.configuration.symbol) }
        .shareInBackground()

    val priceChange = chartState.map { chart ->
        val points = chart.points.dataOrNull.orEmpty()
        if (!hasBittensorSubnetHistoryCoverage(points, chart.period) || !points.first().tao.isPositive) {
            ""
        } else {
            val priceDifference = points.last().tao - points.first().tao
            val percent = priceDifference.multiply(BigDecimal(100))
                .divide(
                    points.first().tao,
                    1,
                    RoundingMode.HALF_EVEN
                )
            (if (percent.isPositive) "+" else "") + percent.toPlainString() + "%"
        }
    }
        .shareInBackground()

    val selectedChartIndex = period.map { BittensorSubnetChartPeriod.entries.indexOf(it) }
        .shareInBackground()

    val charts = combine(
        chartState,
        period,
        chartAssetState
    ) { chart, selected, asset ->
        val state = chart.points
        val points = state.dataOrNull.orEmpty()
        BittensorSubnetChartPeriod.entries.mapIndexed { index, period ->
            val label = resources.getString(period.labelRes())
            if (selected == period && (chart.period != selected || state.isLoading() || asset.isLoading())) {
                PriceChartModel.Loading(label)
            } else if (selected != period || points.isEmpty()) {
                PriceChartModel.Empty(label, "")
            } else {
                PriceChartModel.Chart(
                    label,
                    label,
                    true,
                    points.map { PriceChartModel.Chart.Price(it.timestamp, it.tao) }
                )
            }
        }
    }
        .shareInBackground()

    val positionError = portfolioState.map { portfolio ->
        when {
            portfolio.dataOrNull?.supportsImmediateOperations == false -> resources.getString(R.string.bittensor_multisig_unavailable)
            portfolio.dataOrNull?.dataStatus is BittensorDataStatus.Stale -> resources.getString(R.string.common_retry_message)
            else -> null
        }
    }
        .shareInBackground()

    val choices = MutableLiveData<Event<BittensorPositionChoiceRequest>>()

    private fun loadChart(request: ChartRequest) = flowOf {
        val asset = request.asset
        val priceId = asset?.token?.configuration?.priceId
        if (priceId == null) {
            emptyList()
        } else {
            bittensorSubnetDetailsInteractor.history(
                subnetId,
                priceId,
                asset.token.currency,
                request.period
            )
        }
    }

    fun sellClicked() = launchUnit {
        if (!canSell.first()) return@launchUnit
        val data = portfolio.first() ?: return@launchUnit
        val primary = held.first()
            .sortedBy { it.validatorHotkey.value.toHexString() }
            .sortedByDescending { it.alphaInPlanks }
            .firstOrNull() ?: return@launchUnit
        val address = data.chain.addressOf(primary.validatorHotkey.value)
        router.openBittensorExitAmount(BittensorExitPositionPayload(data.metaId, subnetId.value, address))
    }

    fun buyClicked() = choose(BittensorPositionTradeAction.BUY)

    private fun choose(action: BittensorPositionTradeAction) = launchUnit {
        if (!canAct.first()) return@launchUnit
        val data = portfolio.first() ?: return@launchUnit
        val options = held.first().map { BittensorExitPositionPayload(data.metaId, subnetId.value, data.chain.addressOf(it.validatorHotkey.value)) }
        if (options.size == 1) {
            selectPosition(action, options.single())
        } else {
            choices.value = BittensorPositionChoiceRequest(action, options).event()
        }
    }

    fun selectPosition(action: BittensorPositionTradeAction, payload: BittensorExitPositionPayload) = launchUnit {
        if (!canAct.first()) return@launchUnit
        if (action == BittensorPositionTradeAction.SELL && !canSell.first()) return@launchUnit
        val data = portfolio.first() ?: return@launchUnit
        if (data.metaId != payload.metaId) return@launchUnit

        val containsPosition = held.first().any {
            data.chain.addressOf(it.validatorHotkey.value) == payload.hotkeyAddress
        }

        if (!containsPosition) return@launchUnit
        if (action == BittensorPositionTradeAction.BUY) {
            router.openBittensorSubnetAmount(
                BittensorSubnetAmountPayload(
                    subnetId.value,
                    payload.hotkeyAddress,
                    existingPosition = true
                )
            )
        } else {
            router.openBittensorExitAmount(payload)
        }
    }

    fun validatorClicked(row: BittensorPositionUi) = launchUnit {
        portfolio.first()
            ?.let { externalActions.showAddressActions(row.id, it.chain) }
    }

    fun selectChartPoint(selection: PriceChartSelection?) {
        chartSelection.value = selection
    }

    fun selectPeriod(index: Int) {
        chartSelection.value = null
        BittensorSubnetChartPeriod.entries.getOrNull(index)
            ?.let { period.value = it }
    }

    fun back() = router.back()
}
