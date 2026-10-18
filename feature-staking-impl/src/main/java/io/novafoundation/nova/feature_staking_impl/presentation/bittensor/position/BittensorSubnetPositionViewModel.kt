package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicRecoveryState
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicSubmissionRecovery
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorEstimatesRepository
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimateContext
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorProtocolUnits
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
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetAmountPayload
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartModel
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.addressOf
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
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
    recovery: ExtrinsicSubmissionRecovery,
    private val externalActions: ExternalActions.Presentation,
    private val logger: Logger,
    private val fiatFormatter: FiatFormatter,
    estimatesRepository: BittensorEstimatesRepository,
    private val retryController: BittensorRetryController
) : BaseViewModel(), ExternalActions by externalActions {

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

    private val pending = portfolio.flatMapLatest { data ->
        if (data == null) {
            kotlinx.coroutines.flow.flowOf(ExtrinsicRecoveryState.None)
        } else {
            recovery.observe(data.metaId, ChainGeneses.BITTENSOR)
        }
    }
        .shareInBackground()

    val canAct = combine(
        portfolio,
        held,
        pending,
        metadataState
    ) { data, held, state, subnetMetadataInteractor ->
        data?.supportsImmediateOperations == true && data.dataStatus is BittensorDataStatus.Fresh &&
            held.isNotEmpty() && state !is ExtrinsicRecoveryState.Pending && subnetMetadataInteractor.dataOrNull != null
    }
        .shareInBackground()

    val hasStake = held.map {
        it.maxByOrNull { it.alphaInPlanks }
            ?.isRegistered == true
    }
        .shareInBackground()

    private val costBasisState = estimatesRepository.observeCostBasis(
        portfolio.map { it?.let { BittensorEstimateContext(it.metaId, it.chain) } }
    )
        .shareInBackground()

    private val yieldState = estimatesRepository.observeAlphaYields(
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
        chartAssetState
    ) { subnet, assetState ->
        val asset = assetState.dataOrNull
        if (subnet?.priceInRao == null || asset == null) {
            "—"
        } else {
            tokenFormatter.formatToken(asset.token.amountFromPlanks(subnet.priceInRao), asset.token.configuration.symbol)
        }
    }
        .shareInBackground()

    val averagePrice = combine(
        costBasisState,
        chartAssetState
    ) { state, assetState ->
        state.map { basis ->
            val average = basis?.forSubnet(subnetId.value)
                ?.averagePrice
            val asset = assetState.dataOrNull
            if (average == null || asset == null) "—" else "≈ " + tokenFormatter.formatToken(average, asset.token.configuration.symbol)
        }
    }
        .shareInBackground()

    val earned = combine(
        costBasisState,
        portfolio,
        held
    ) { state, data, positions ->
        state.map { basis ->
            val totals = basis?.forSubnet(subnetId.value)
            val worth = data?.positions?.subnetStake(subnetId)
            if (totals == null || worth == null) {
                "—"
            } else {
                val alpha = positions.fold(BigInteger.ZERO) { sum, position -> sum + position.alphaInPlanks }
                "≈ " + tokenFormatter.formatToken(
                    data.asset.token.amountFromPlanks(totals.estimatedProfit(alpha, worth)),
                    data.asset.token.configuration.symbol
                )
            }
        }
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

    val worth = portfolio.map { data ->
        val planks = data?.positions?.subnetStake(subnetId)
        if (planks == null || data == null) {
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
                BittensorPositionUi(
                    address,
                    address.take(6) + "…" + address.takeLast(6),
                    resources.getString(R.string.bittensor_validator),
                    AmountModel(
                        yields?.get(position.validatorHotkey)
                            ?.let { "≈ ${it.stripTrailingZeros().toPlainString()}%" } ?: "—",
                        resources.getString(R.string.bittensor_apy)
                    ),
                    Icon.FromDrawable(addressIconGenerator.createAddressIcon(position.validatorHotkey.value, 32)),
                    BittensorPositionAction.ValidatorDetails(address),
                    32
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
        }
        .shareInBackground()

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

    val axisFormatter = chartAssetState
        .mapNotNull { it.dataOrNull }
        .map { BittensorPositionChartFormatter(tokenFormatter, it.token.configuration.symbol) }
        .shareInBackground()

    val priceChange = chartState.map { state ->
        val points = state.dataOrNull.orEmpty()
        if (points.size < 2 || !points.first().tao.isPositive) {
            "—"
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
    ) { state, selected, asset ->
        val points = state.dataOrNull.orEmpty()
        BittensorSubnetChartPeriod.entries.mapIndexed { index, period ->
            val label = resources.getString(period.labelRes())
            if (selected == period && (state.isLoading() || asset.isLoading())) {
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

    val positionError = combine(
        portfolioState,
        metadataState,
        pending
    ) { portfolio, subnetMetadataInteractor, pending ->
        when {
            pending is ExtrinsicRecoveryState.Pending -> resources.getString(R.string.bittensor_submission_unknown_description)
            portfolio.dataOrNull?.supportsImmediateOperations == false -> resources.getString(R.string.bittensor_multisig_unavailable)
            portfolio.dataOrNull?.dataStatus is BittensorDataStatus.Stale -> resources.getString(R.string.common_retry_message)
            else -> null
        }
    }
        .shareInBackground()

    val choices = MutableLiveData<Event<BittensorPositionChoiceRequest>>()

    fun sellClicked() = choose(BittensorPositionTradeAction.SELL)

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

    fun selectPeriod(index: Int) {
        BittensorSubnetChartPeriod.entries.getOrNull(index)
            ?.let { period.value = it }
    }

    fun back() = router.back()
}
