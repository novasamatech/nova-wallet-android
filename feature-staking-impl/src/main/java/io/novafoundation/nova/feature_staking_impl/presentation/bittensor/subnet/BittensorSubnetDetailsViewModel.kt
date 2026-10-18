package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorLoadingValue
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.isLoaded
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetChart
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetChartPeriod
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetChartRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetDetailsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPricePoint
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetSafetyInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorChartAxisFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.labelRes
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorChartChangeTextInjector
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatToken
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartModel
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach

class BittensorSubnetDetailsViewModel(
    private val subnetId: BittensorNetuid.Subnet,
    private val bittensorSubnetInteractor: BittensorSubnetInteractor,
    private val bittensorSubnetStakingInteractor: BittensorSubnetStakingInteractor,
    private val router: StakingRouter,
    private val resources: ResourceManager,
    private val tokenFormatter: TokenFormatter,
    private val iconGenerator: AddressIconGenerator,
    private val bittensorSubnetDetailsInteractor: BittensorSubnetDetailsInteractor,
    private val logger: Logger,
    private val validatorCommunicator: BittensorSelectValidatorCommunicator,
    private val fiatFormatter: FiatFormatter,
    private val chartAxisFormatterFactory: BittensorChartAxisFormatter.Factory,
    private val retryController: BittensorRetryController,
    private val subnetSafetyInteractor: BittensorSubnetSafetyInteractor
) : BaseViewModel() {

    val retryEvents = retryController.events

    private val subnetState = retryController.observe(
        step = "subnet",
        transform = { it.withSafeLoading() }
    ) {
        flowOf { bittensorSubnetInteractor.subnet(subnetId) }
    }
        .shareInBackground()

    val subnet = subnetState.mapNotNull { it.dataOrNull }
        .shareInBackground()

    private val validatorsState = retryController.observe(
        step = "validators",
        transform = { it.withSafeLoading() }
    ) {
        flowOf { bittensorSubnetStakingInteractor.validators(subnetId) }
    }
        .shareInBackground()

    private val validators = validatorsState.map { it.dataOrNull.orEmpty() }
        .shareInBackground()

    private val selectedValidatorAddress = MutableStateFlow<String?>(null)

    val selectedValidator = combine(
        validators,
        selectedValidatorAddress
    ) { validators, address ->
        validators.firstOrNull { it.address == address && it.selectable }
    }
        .shareInBackground()

    private val presetSelection = MutableStateFlow(BittensorStakeAmountPreset.FIVE)

    val selectedPreset = presetSelection.asStateFlow()

    private val _isFiatMode = MutableStateFlow(false)

    val isFiatMode = _isFiatMode.asStateFlow()

    private val period = MutableStateFlow(BittensorSubnetChartPeriod.WEEK)

    val selectedChartIndex = period.map { BittensorSubnetChartPeriod.entries.indexOf(it) }
        .shareInBackground()

    private val assetState = retryController.observe(
        step = "asset",
        transform = { it.withSafeLoading() },
        source = bittensorSubnetInteractor::observeAsset
    )
        .shareInBackground()

    private val asset = assetState.mapNotNull { it.dataOrNull }
        .shareInBackground()

    val nativeTokenSymbol = asset.map { it.token.configuration.symbol.value }
        .shareInBackground()

    val presetLabels = asset.map { asset ->
        BittensorStakeAmountPreset.entries.associateWith { preset ->
            when (preset) {
                BittensorStakeAmountPreset.MAX -> resources.getString(R.string.bittensor_max)
                else -> tokenFormatter.formatToken(preset.amountFor(asset.transferable), asset.token.configuration.symbol)
            }
        }
    }
        .shareInBackground()

    private val presetAmount = combine(
        selectedPreset,
        asset
    ) { preset, asset ->
        preset.amountFor(asset.transferable)
    }
        .shareInBackground()

    val favorite = bittensorSubnetInteractor.observeFavorites().map { subnetId.value in it.subnetIds }
        .shareInBackground()

    val canContinue = combine(
        subnetState,
        validatorsState,
        assetState
    ) { subnet, validators, asset ->
        subnet.isLoaded() && validators.isLoaded() && asset.isLoaded()
    }
        .shareInBackground()

    val validatorIcon = selectedValidator.map { selected ->
        selected?.let {
            iconGenerator.createAddressIcon(it.validator.hotkey.value, 24, AddressIconGenerator.BACKGROUND_TRANSPARENT)
        }
    }
        .shareInBackground()

    val validatorCount = validatorsState.mapLoading { it.size.toString() }
        .shareInBackground()

    val pool = combine(
        subnetState,
        assetState
    ) { subnetState, assetState ->
        bittensorLoadingValue(subnetState, assetState) {
            val subnet = subnetState.dataOrNull
            val asset = assetState.dataOrNull
            if (subnet?.taoReserveInPlanks == null || asset == null) {
                "—"
            } else {
                tokenFormatter.formatToken(subnet.taoReserveInPlanks, asset)
            }
        }
    }
        .shareInBackground()

    val estimatedAlpha = combine(
        subnet,
        presetAmount,
        asset
    ) { subnet, amount, asset ->
        bittensorSubnetDetailsInteractor.estimateAlpha(amount, subnet.priceInRao, asset.token.configuration)
            ?.let {
                resources.getString(
                    R.string.bittensor_estimated_hold,
                    tokenFormatter.formatToken(
                        it,
                        TokenSymbol(subnet.symbol.ifBlank { "—" })
                    )
                )
            } ?: "—"
    }
        .shareInBackground()

    val price = combine(
        subnet,
        asset,
        isFiatMode
    ) { subnet, asset, fiat ->
        val price = subnet.priceInRao?.let(asset.token::amountFromPlanks) ?: return@combine "—"
        if (fiat) {
            val fiatPrice = asset.token.toFiatOrNull(price) ?: return@combine "—"
            fiatFormatter.formatFiat(fiatPrice, asset.token.currency)
        } else {
            tokenFormatter.formatToken(price, asset.token.configuration.symbol)
        }
    }
        .shareInBackground()

    val currencyLabel = asset.map { it.token.currency.code }
        .shareInBackground()

    val axisFormatter = combine(
        asset,
        isFiatMode
    ) { asset, isFiatMode ->
        chartAxisFormatterFactory.create(asset.token, isFiatMode)
    }
        .shareInBackground()

    private val chartPeriods = BittensorSubnetChartPeriod.entries

    private val chartData = combine(
        asset,
        period
    ) { asset, period ->
        BittensorSubnetChartRequest(asset.token.configuration.priceId, asset.token.currency, period)
    }
        .distinctUntilChanged().flatMapLatest { request ->
            retryController.observe(
                step = "subnet chart",
                transform = { it.withSafeLoading() }
            ) {
                flowOf { loadChartHistory(request) }
            }
                .map { BittensorSubnetChart(request.period, it) }
        }
        .shareInBackground()

    val charts = combine(chartData, isFiatMode, ::mapCharts)
        .shareInBackground()

    val priceChangeFormatter = period.map { BittensorChartChangeTextInjector(periodLabel(it)) }
        .shareInBackground()

    private val ageState = subnetState.flatMapLatest { state ->
        val subnet = state.dataOrNull
        if (subnet == null) {
            flowOf { state.map { null } }
        } else {
            retryController.observe(
                step = "subnet age",
                transform = { it.withSafeLoading() }
            ) {
                flowOf { subnetSafetyInteractor.ageInDays(subnet) }
            }
        }
    }
        .shareInBackground()

    val age = ageState.mapLoading {
        it?.let { days ->
            resources.getString(
                R.string.bittensor_safety_age_days,
                days.toBigInteger().toString()
            )
        } ?: "—"
    }
        .shareInBackground()

    private val safetyHistory = assetState.flatMapLatest { state ->
        val asset = state.dataOrNull
        if (asset == null) {
            flowOf { state.map { emptyList<BittensorSubnetPricePoint>() } }
        } else {
            retryController.observe(
                step = "subnet safety history",
                transform = { it.withSafeLoading() }
            ) {
                flowOf {
                    val priceId = asset.token.configuration.priceId ?: return@flowOf emptyList()
                    bittensorSubnetDetailsInteractor.history(
                        subnetId,
                        priceId,
                        asset.token.currency,
                        BittensorSubnetChartPeriod.MONTH
                    )
                }
            }
        }
    }
        .shareInBackground()

    val priceHistory = safetyHistory.mapLoading { state ->
        state?.takeIf { it.size >= 2 }
            ?.let { points ->
                val days = (points.last().timestamp - points.first().timestamp) / 86_400
                resources.getString(R.string.bittensor_safety_observed_days, days.toString())
            } ?: "—"
    }
        .shareInBackground()

    val priceRange = combine(
        safetyHistory,
        assetState
    ) { state, assetState ->
        bittensorLoadingValue(state, assetState) {
            val asset = assetState.dataOrNull ?: return@bittensorLoadingValue "—"
            val history = state.dataOrNull
            history?.takeIf { it.size >= 2 }
                ?.let { points ->
                    val low = tokenFormatter.formatToken(
                        points.minOf { it.tao },
                        asset.token.configuration.symbol
                    )
                    val high = tokenFormatter.formatToken(
                        points.maxOf { it.tao },
                        asset.token.configuration.symbol
                    )
                    "$low – $high"
                } ?: "—"
        }
    }
        .shareInBackground()

    init {
        combine(
            validatorCommunicator.responseFlow,
            validatorsState
        ) { response, validators ->
            validators.dataOrNull?.firstOrNull { response.subnetId == subnetId.value && it.address == response.address }
        }
            .mapNotNull { it }
            .onEach(::validatorSelected)
            .launchIn(this)
    }

    private fun periodLabel(period: BittensorSubnetChartPeriod): String = resources.getString(period.labelRes())

    fun validatorClicked() = launchUnit {
        if (canContinue.first()) {
            validatorCommunicator.openRequest(
                BittensorSelectValidatorCommunicator.Request(
                    subnetId.value,
                    selectedValidator.first()
                        ?.address
                )
            )
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
        chartPeriods.getOrNull(index)
            ?.let { period.value = it }
    }

    fun toggleFavorite() = launchUnit {
        try {
            val favorites = bittensorSubnetInteractor.observeFavorites()
                .first()
            bittensorSubnetInteractor.toggleFavorite(favorites.metaId, subnetId)
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
            router.openBittensorSubnetAmount(BittensorSubnetAmountPayload(subnetId.value, selected.address))
        }
    }

    fun back() = router.back()

    private suspend fun loadChartHistory(request: BittensorSubnetChartRequest): List<BittensorSubnetPricePoint> {
        val priceId = request.nativePriceId ?: return emptyList()
        return bittensorSubnetDetailsInteractor.history(
            subnetId,
            priceId,
            request.currency,
            request.period
        )
    }

    private fun mapCharts(data: BittensorSubnetChart, isFiatMode: Boolean): List<PriceChartModel> = chartPeriods.map { period ->
        mapChart(data, period, isFiatMode)
    }

    private fun mapChart(data: BittensorSubnetChart, period: BittensorSubnetChartPeriod, isFiatMode: Boolean): PriceChartModel {
        val label = periodLabel(period)
        if (period != data.period) return PriceChartModel.Loading(label)

        return when (val state = data.points) {
            is ExtendedLoadingState.Loading -> PriceChartModel.Loading(label)
            is ExtendedLoadingState.Error -> PriceChartModel.Empty(label, "")
            is ExtendedLoadingState.Loaded -> {
                if (state.data.isEmpty()) return PriceChartModel.Empty(label, "")

                val prices = state.data.map { PriceChartModel.Chart.Price(it.timestamp, if (isFiatMode) it.fiat else it.tao) }
                PriceChartModel.Chart(
                    buttonText = label,
                    periodName = label,
                    supportTimeShowing = true,
                    priceChart = prices
                )
            }
        }
    }

    private fun reportFailure(
        step: String,
        cause: Throwable
    ) {
        logger.log("[BittensorSubnetDetails] Failed to load $step")
        logger.log(cause)
        showError(resources.getString(R.string.common_retry_message))
    }
}
