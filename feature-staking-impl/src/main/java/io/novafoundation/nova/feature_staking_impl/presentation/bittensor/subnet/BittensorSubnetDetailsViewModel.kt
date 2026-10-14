package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.isLoaded
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.domain.onError
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorChartAxisFormatter
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetChartPeriod
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetDetailsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatToken
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartModel
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformLatest
import java.math.BigDecimal

class BittensorSubnetDetailsViewModel(
    private val subnetId: BittensorSubnetId,
    private val bittensorSubnetInteractor: BittensorSubnetInteractor,
    private val bittensorSubnetStakingInteractor: BittensorSubnetStakingInteractor,
    private val router: StakingRouter,
    private val resources: ResourceManager,
    private val formatter: TokenFormatter,
    private val iconGenerator: AddressIconGenerator,
    private val bittensorSubnetDetailsInteractor: BittensorSubnetDetailsInteractor,
    private val logger: Logger,
    private val validatorCommunicator: BittensorSelectValidatorCommunicator,
    private val fiatFormatter: FiatFormatter,
    private val chartAxisFormatterFactory: BittensorChartAxisFormatter.Factory
) : BaseViewModel() {

    private val subnetState = flowOf { bittensorSubnetInteractor.subnet(subnetId) }
        .withSafeLoading()
        .onError { reportFailure("subnet", it) }
        .shareInBackground()

    val subnet = subnetState.mapNotNull { it.dataOrNull }.shareInBackground()

    private val validatorsState = flowOf { bittensorSubnetStakingInteractor.validators(subnetId) }
        .withSafeLoading()
        .onError { reportFailure("validators", it) }
        .shareInBackground()

    private val validators = validatorsState.mapNotNull { it.dataOrNull }.shareInBackground()

    val isLoading = combine(subnetState, validatorsState) { subnet, validators -> subnet.isLoading() || validators.isLoading() }
        .shareInBackground()

    private val selectedValidatorAddress = MutableStateFlow<String?>(null)

    val selectedValidator = combine(validators, selectedValidatorAddress) { validators, address ->
        validators.firstOrNull { it.address == address && it.selectable }
    }.shareInBackground()

    private val presetSelection = MutableStateFlow(BittensorStakeAmountPreset.FIVE)

    val selectedPreset = presetSelection.asStateFlow()

    private val _isFiatMode = MutableStateFlow(false)

    val isFiatMode = _isFiatMode.asStateFlow()

    private val period = MutableStateFlow(BittensorSubnetChartPeriod.WEEK)

    val selectedChartIndex = period.map { BittensorSubnetChartPeriod.entries.indexOf(it) }.shareInBackground()

    private val portfolio = bittensorSubnetInteractor.observePortfolio().catch { reportFailure("portfolio", it) }.shareInBackground()

    private val asset = bittensorSubnetInteractor.observeAsset().catch { reportFailure("asset", it) }.shareInBackground()

    val nativeTokenSymbol = asset.map { it.token.configuration.symbol.value }.shareInBackground()

    val presetLabels = asset.map { asset ->
        BittensorStakeAmountPreset.entries.associateWith { preset ->
            when (preset) {
                BittensorStakeAmountPreset.MAX -> resources.getString(R.string.bittensor_max)
                else -> formatter.formatToken(preset.amountFor(asset.transferable), asset.token.configuration.symbol)
            }
        }
    }.shareInBackground()

    val presetAmount = combine(selectedPreset, asset) { preset, asset ->
        preset.amountFor(asset.transferable)
    }.shareInBackground()

    val favorite = bittensorSubnetInteractor.observeFavorites().map { subnetId.value in it.subnetIds }.shareInBackground()

    val canContinue = combine(subnetState, selectedValidator) { subnet, selected -> subnet.isLoaded() && selected != null }
        .shareInBackground()

    val validatorIcon = selectedValidator.map { selected ->
        selected?.let {
            iconGenerator.createAddressIcon(it.validator.hotkey.value, 24, AddressIconGenerator.BACKGROUND_TRANSPARENT)
        }
    }.shareInBackground()

    val validatorCount = validators.map { it.size.toString() }.shareInBackground()

    val pool = combine(subnet, asset) { subnet, asset ->
        subnet.taoReserveInPlanks?.let { formatter.formatToken(it, asset) } ?: "—"
    }
        .shareInBackground()

    val estimatedAlpha = combine(subnet, presetAmount) { subnet, amount ->
        bittensorSubnetDetailsInteractor.estimateAlpha(amount, subnet.priceInRao)?.let {
            resources.getString(R.string.bittensor_estimated_hold, formatter.formatToken(it, TokenSymbol(subnet.symbol.ifBlank { "—" })))
        } ?: "—"
    }.shareInBackground()

    val price = combine(subnet, asset, isFiatMode) { subnet, asset, fiat ->
        val price = subnet.priceInRao?.let(asset.token::amountFromPlanks)
        if (fiat) {
            price?.let(asset.token::toFiatOrNull)?.let { fiatFormatter.formatFiat(it, asset.token.currency) } ?: "—"
        } else {
            price?.let { formatter.formatToken(it, asset.token.configuration.symbol) } ?: "—"
        }
    }.shareInBackground()

    val currencyLabel = asset.map { it.token.currency.code }.shareInBackground()

    val axisFormatter = combine(asset, isFiatMode) { asset, isFiatMode ->
        chartAxisFormatterFactory.create(asset.token, isFiatMode)
    }.shareInBackground()

    private val chartPoints = combine(asset, period) { asset, period -> Triple(asset.token.configuration.priceId, asset.token.currency, period) }
        .distinctUntilChanged()
        .transformLatest { (priceId, currency, period) ->
            emit(emptyList())
            priceId ?: return@transformLatest
            try {
                emit(bittensorSubnetDetailsInteractor.history(subnetId, priceId, currency, period))
            } catch (e: CancellationException) {
                if (e !is TimeoutCancellationException) throw e
                logger.log("[BittensorSubnetDetails] Chart unavailable")
                logger.log(e)
            } catch (e: Exception) {
                logger.log("[BittensorSubnetDetails] Chart unavailable")
                logger.log(e)
            }
        }.shareInBackground()

    private val periods = BittensorSubnetChartPeriod.entries

    private val labels = listOf("1D", "7D", "1M", "3M", "1Y")

    val charts = combine(chartPoints, period, _isFiatMode) { points, selected, fiat ->
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

    val priceChange = combine(chartPoints, _isFiatMode, period) { points, fiat, period ->
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
        combine(validatorCommunicator.responseFlow, validatorsState) { response, validators ->
            validators.dataOrNull?.firstOrNull { response.subnetId == subnetId.value && it.address == response.address }
        }.onEach { row -> row?.let(::validatorSelected) }.launchIn(this)
    }

    fun validatorClicked() = launchUnit {
        if (!isLoading.first()) {
            validatorCommunicator.openRequest(BittensorSelectValidatorCommunicator.Request(subnetId.value, selectedValidator.first()?.address))
        }
    }

    private fun validatorSelected(row: BittensorSubnetValidator) {
        if (row.selectable) selectedValidatorAddress.value = row.address
    }

    fun selectPreset(value: BittensorStakeAmountPreset) {
        presetSelection.value = value
    }

    fun showFiat(value: Boolean) {
        _isFiatMode.value = value
    }

    fun selectPeriod(index: Int) {
        periods.getOrNull(index)?.let { period.value = it }
    }

    fun toggleFavorite() = launchUnit {
        try {
            bittensorSubnetInteractor.toggleFavorite(portfolio.first().metaId, subnetId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reportFailure("favorite", e)
        }
    }

    fun continueClicked() = launchUnit {
        val selected = selectedValidator.first()
        if (selected == null) {
            validatorClicked()
        } else if (canContinue.first()) {
            router.openBittensorSubnetAmount(subnetId.value, selected.address)
        }
    }

    fun back() = router.back()

    private fun reportFailure(step: String, cause: Throwable) {
        logger.log("[BittensorSubnetDetails] Failed to load $step")
        logger.log(cause)
        showError(resources.getString(R.string.common_retry_message))
    }
}
