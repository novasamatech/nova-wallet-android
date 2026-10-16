package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import kotlinx.coroutines.flow.onStart
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.repository.expectedBlockTime
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.ext.ChainGeneses
import kotlinx.coroutines.flow.flatMapLatest
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetDetailsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetChartPeriod
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartModel
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import java.math.BigDecimal

class BittensorSubnetDetailsViewModel(
    private val subnetId: BittensorSubnetId,
    private val interactor: BittensorSubnetInteractor,
    private val staking: BittensorSubnetStakingInteractor,
    private val router: StakingRouter,
    private val resources: ResourceManager,
    private val formatter: TokenFormatter,
    private val iconGenerator: AddressIconGenerator,
    private val details: BittensorSubnetDetailsInteractor,
    private val logger: Logger,
    private val validatorCommunicator: BittensorSelectValidatorCommunicator,
    private val rpc: RpcCalls,
    private val chainState: ChainStateRepository
) : BaseViewModel() {
    private val subnetState = MutableStateFlow<BittensorSubnet?>(null)
    val subnet = subnetState.asStateFlow()
    private val validators = MutableStateFlow<List<BittensorValidatorUi>>(emptyList())
    private val selection = MutableStateFlow<BittensorSubnetValidator?>(null)
    val selectedValidator = selection.asStateFlow()
    private val preset = MutableStateFlow(BigDecimal(5))
    val presetAmount = preset.asStateFlow()
    private val presetSelection = MutableStateFlow(5)
    val selectedPreset = presetSelection.asStateFlow()
    private val fiat = MutableStateFlow(false)
    val isFiat = fiat.asStateFlow()
    private val period = MutableStateFlow(BittensorSubnetChartPeriod.WEEK)
    val selectedChartIndex = period.map { BittensorSubnetChartPeriod.entries.indexOf(it) }.shareInBackground()
    private val loading = MutableStateFlow(false)
    val isLoading = loading.asStateFlow()
    private val portfolio = interactor.observePortfolio().catch { reportFailure("portfolio", it) }.shareInBackground()
    val favorite = interactor.observeFavorites().map { subnetId.value in it.subnetIds }.shareInBackground()
    val canContinue = combine(subnet, selection, loading) { subnet, selected, loading -> subnet != null && selected?.selectable == true && !loading }
        .shareInBackground()
    val validatorIcon = combine(validators, selection) { rows, selected -> rows.firstOrNull { it.validator == selected }?.icon }.shareInBackground()
    val validatorCount = validators.map { it.size.toString() }.shareInBackground()
    val pool = subnet.map { it?.taoReserveInPlanks?.let { value -> formatter.formatToken(BigDecimal(value, 9), TokenSymbol("TAO")) } ?: "—" }
        .shareInBackground()
    val estimatedAlpha = combine(subnet, preset) { subnet, amount ->
        details.estimateAlpha(amount, subnet?.priceInRao)?.let {
            resources.getString(R.string.bittensor_estimated_hold, formatter.formatToken(it, TokenSymbol(subnet?.symbol.orEmpty().ifBlank { "—" })))
        } ?: "—"
    }.shareInBackground()
    val price = combine(subnet, portfolio, fiat) { subnet, portfolio, fiat ->
        val price = subnet?.priceInRao?.let { BigDecimal(it, 9) }
        if (fiat) {
            price?.let(portfolio.asset.token::toFiatOrNull)?.formatAsCurrency(portfolio.asset.token.currency) ?: "—"
        } else {
            price?.let { formatter.formatToken(it, TokenSymbol("TAO")) } ?: "—"
        }
    }.shareInBackground()
    val currencyLabel = portfolio.map { it.asset.token.currency.code.uppercase() }.shareInBackground()
    val axisFormatter = combine(portfolio, fiat) { portfolio, fiat ->
        val format: (Float) -> String = { value ->
            if (fiat) {
                value.toBigDecimal().formatAsCurrency(portfolio.asset.token.currency)
            } else {
                formatter.formatToken(value.toBigDecimal(), TokenSymbol("TAO")).toString()
            }
        }
        format
    }.shareInBackground()
    private val chartAsset = interactor.observeAsset().catch { reportFailure("chart asset", it) }.shareInBackground()
    private val chartPoints = combine(chartAsset, period) { asset, period -> Triple(asset.token.configuration.priceId, asset.token.currency, period) }
        .distinctUntilChanged()
        .transformLatest { (priceId, currency, period) ->
            emit(emptyList())
            priceId ?: return@transformLatest
            try {
                emit(details.history(subnetId, priceId, currency, period))
            } catch (e: CancellationException) {
                if (e !is TimeoutCancellationException) throw e
                logger.log("[BittensorSubnetDetails] Chart unavailable")
                logger.log(e)
            } catch (e: Exception) {
                logger.log("[BittensorSubnetDetails] Chart unavailable")
                logger.log(e)
            }
        }.shareInBackground()
    private val safetyReload = MutableStateFlow(0L)

    private val ageState = combine(subnet, safetyReload) { subnet, _ -> subnet?.registrationBlock }
        .flatMapLatest { registration ->
            flowOf {
                registration ?: return@flowOf null
                val latest = java.math.BigInteger.valueOf(rpc.getBlockHeader(ChainGeneses.BITTENSOR).number.toLong())
                require(latest >= registration) { "Registration block is ahead of the chain" }
                val blockTime = chainState.expectedBlockTime(ChainGeneses.BITTENSOR)
                require(blockTime.isPositive())
                (latest - registration).toBigDecimal() * blockTime.inWholeMilliseconds.toBigDecimal() / BigDecimal(86_400_000)
            }.onStart { emit(null) }
                .catch { cause ->
                    logger.log("[BittensorSubnetDetails] Safety data unavailable")
                    logger.log(cause)
                    emit(null)
                }
        }.shareInBackground()

    val age = ageState.map { it?.let { days -> resources.getString(R.string.bittensor_safety_age_days, days.toBigInteger().toString()) } ?: "—" }
        .shareInBackground()

    private val safetyHistory = combine(chartAsset, safetyReload) { asset, _ -> asset }
        .flatMapLatest { asset ->
            flowOf<List<io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPricePoint>?> {
                val priceId = asset.token.configuration.priceId ?: return@flowOf emptyList()
                details.history(subnetId, priceId, asset.token.currency, BittensorSubnetChartPeriod.MONTH)
            }.onStart { emit(null) }
                .catch { cause ->
                    logger.log("[BittensorSubnetDetails] Safety data unavailable")
                    logger.log(cause)
                    emit(null)
                }
        }.shareInBackground()

    val priceHistory = safetyHistory.map { state ->
        state?.takeIf { it.size >= 2 }?.let { points ->
            val days = (points.last().timestamp - points.first().timestamp) / 86_400
            resources.getString(R.string.bittensor_safety_observed_days, days.toString())
        } ?: "—"
    }.shareInBackground()

    val priceRange = combine(safetyHistory, chartAsset) { state, asset ->
        state?.takeIf { it.size >= 2 }?.let { points ->
            val low = formatter.formatToken(points.minOf { it.tao }, asset.token.configuration.symbol)
            val high = formatter.formatToken(points.maxOf { it.tao }, asset.token.configuration.symbol)
            "$low – $high"
        } ?: "—"
    }.shareInBackground()

    fun retrySafety() { safetyReload.value++ }

    private val periods = BittensorSubnetChartPeriod.entries
    private val labels = listOf("1D", "7D", "1M", "3M", "1Y")
    val charts = combine(chartPoints, period, fiat) { points, selected, fiat ->
        periods.mapIndexed { index, period ->
            if (period != selected || points.isEmpty()) {
                PriceChartModel.Empty(labels[index], "")
            } else {
                PriceChartModel.Chart(
                    labels[index],
                    labels[index],
                    true,
                    points.map {
                        PriceChartModel.Chart.Price(it.timestamp, if (fiat) it.fiat else it.tao)
                    }
                )
            }
        }
    }.shareInBackground()
    val priceChange = combine(chartPoints, fiat, period) { points, fiat, period ->
        if (points.size < 2) {
            "—"
        } else {
            val first = if (fiat) points.first().fiat else points.first().tao
            val last = if (fiat) points.last().fiat else points.last().tao
            val percent = (last - first).multiply(BigDecimal(100)).divide(first, 1, java.math.RoundingMode.HALF_EVEN)
            (if (percent.signum() > 0) "+" else "") + percent.toPlainString() + "% · " + labels[periods.indexOf(period)]
        }
    }.shareInBackground()

    init {
        combine(validatorCommunicator.responseFlow, validators) { response, rows ->
            rows.firstOrNull { response.subnetId == subnetId.value && it.validator.address == response.address }?.validator
        }.onEach { row -> row?.let(::validatorSelected) }.launchIn(this)
        launchUnit {
            loading.value = true
            try {
                subnetState.value = interactor.subnets().first { it.id == subnetId }
                validators.value = staking.validators(subnetId).map { row ->
                    BittensorValidatorUi(row, iconGenerator.createAddressIcon(row.validator.hotkey.value, 24, AddressIconGenerator.BACKGROUND_TRANSPARENT))
                }
            } catch (e: CancellationException) {
                if (e !is TimeoutCancellationException) throw e
                reportFailure("catalogue or validators", e)
            } catch (e: Exception) {
                reportFailure("catalogue or validators", e)
            } finally {
                loading.value = false
            }
        }
    }

    fun validatorClicked() {
        if (!loading.value) validatorCommunicator.openRequest(BittensorSelectValidatorCommunicator.Request(subnetId.value, selection.value?.address))
    }

    fun validatorSelected(row: BittensorSubnetValidator) {
        if (row.selectable && validators.value.any { it.validator == row }) selection.value = row
    }

    fun selectPreset(value: Int) {
        preset.value = BigDecimal(value)
        presetSelection.value = value
    }
    fun maxClicked() = launchUnit {
        preset.value = portfolio.first().asset.transferable
        presetSelection.value = 0
    }
    fun showFiat(value: Boolean) { fiat.value = value }
    fun selectPeriod(index: Int) { periods.getOrNull(index)?.let { period.value = it } }
    fun toggleFavorite() = launchUnit {
        try {
            interactor.toggleFavorite(portfolio.first().metaId, subnetId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reportFailure("favorite", e)
        }
    }
    fun continueClicked() {
        val selected = selection.value ?: return validatorClicked()
        if (!loading.value && selected.selectable) router.openBittensorSubnetAmount(subnetId.value, selected.address)
    }
    fun back() = router.back()
    private fun reportFailure(step: String, cause: Throwable) {
        logger.log("[BittensorSubnetDetails] Failed to load $step")
        logger.log(cause)
        showError(resources.getString(R.string.common_retry_message))
    }
}
