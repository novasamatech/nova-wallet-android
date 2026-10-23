package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.SubnetDetailsSource
import io.novafoundation.nova.analytics.AnalyticsPricePeriod
import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.analytics.StakingAnalyticsSelectionMethod
import io.novafoundation.nova.analytics.BittensorDataEndpoint
import io.novafoundation.nova.analytics.BittensorScreen
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorReadAnalytics
import io.novafoundation.nova.analytics.AnalyticsService

import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetLinks
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.hasBittensorSubnetHistoryCoverage
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.presentation.ellipsizeAddress
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPresetValidatorInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorPreview
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.selectBittensorValidatorPreview
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorYieldColor
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorPositionAction
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorPositionUi
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.ext.addressOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.isLoaded
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.formatAsPercentage
import io.novafoundation.nova.common.utils.formatting.formatFractionAsPercentage
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.ltrIsolated
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BITTENSOR_ALPHA_DECIMALS
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.bittensorRegistrationEpochSeconds
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.clipBittensorSubnetHistory
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetChartPeriod
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetChartRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetDetailsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetEmission
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetParametersInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPricePoint
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetReference
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetSafetyInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.bittensorAlphaPoolValueInRao
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.bittensorPoolNativeShare
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorChartAxisFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorLoadingValue
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.formatBittensorSubnetAge
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.labelRes
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorChartChangeTextInjector
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.PriceChartSelection
import io.novafoundation.nova.feature_wallet_api.presentation.view.priceCharts.formatters.RealDateChartTextInjector
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class BittensorSubnetDetailsViewModel(
    private val analytics: AnalyticsService,
    private val subnetId: BittensorNetuid.Subnet,
    private val bittensorSubnetInteractor: BittensorSubnetInteractor,
    private val bittensorSubnetStakingInteractor: BittensorSubnetStakingInteractor,
    private val router: StakingRouter,
    private val resources: ResourceManager,
    private val tokenFormatter: TokenFormatter,
    private val bittensorSubnetDetailsInteractor: BittensorSubnetDetailsInteractor,
    private val logger: Logger,
    private val fiatFormatter: FiatFormatter,
    private val chartAxisFormatterFactory: BittensorChartAxisFormatter.Factory,
    private val retryController: BittensorRetryController,
    private val subnetSafetyInteractor: BittensorSubnetSafetyInteractor,
    private val subnetParametersInteractor: BittensorSubnetParametersInteractor,
    private val validatorInteractor: BittensorValidatorInteractor,
    private val presetValidatorInteractor: BittensorPresetValidatorInteractor,
    private val validatorCommunicator: BittensorSelectValidatorCommunicator,
    private val addressIconGenerator: AddressIconGenerator,
    private val replaceAmount: Boolean,
    initialValidatorAddress: String?,
    initialSelectionState: BittensorSelectionState = BittensorSelectionState(),
    private val analyticsSource: SubnetDetailsSource = SubnetDetailsSource.PICKER
) : BaseViewModel(), Browserable.Presentation by Browserable() {

    private var selectionState = initialSelectionState

    private val chartSelection = MutableStateFlow<PriceChartSelection?>(null)

    val chartDateFormatter = RealDateChartTextInjector(resources)

    private val subnetsReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.SUBNETS, BittensorScreen.SUBNET_DETAILS)
    private val validatorsReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.VALIDATORS, BittensorScreen.SUBNET_DETAILS)
    private val chainRpcReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.CHAIN_RPC, BittensorScreen.SUBNET_DETAILS)
    private val priceServerReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.PRICE_SERVER, BittensorScreen.SUBNET_DETAILS)

    val retryEvents = retryController.events

    private val subnetState = retryController.observe(
        step = "subnet",
        analytics = subnetsReads,
        transform = { it.withSafeLoading() }
    ) {
        flowOf { bittensorSubnetInteractor.subnet(subnetId) }
    }
        .shareInBackground()

    val subnet = subnetState.mapNotNull { it.dataOrNull }
        .shareInBackground()

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

    val favorite = combine(subnet, bittensorSubnetInteractor.observeFavorites()) { subnet, favorites -> subnet in favorites }
        .shareInBackground()

    val canContinue = subnetState.map { it.isLoaded() }
        .shareInBackground()

    val validatorCount = retryController.observe(
        step = "subnet validator count",
        analytics = validatorsReads,
        transform = { it.withSafeLoading() }
    ) {
        flowOf { bittensorSubnetStakingInteractor.validatorCount(subnetId).toString() }
    }
        .shareInBackground()

    private val emissionState = subnetState.flatMapLatest { state ->
        val subnet = state.dataOrNull
        val registration = subnet?.registrationBlock
        if (subnet == null || registration == null) {
            flowOf<ExtendedLoadingState<BittensorSubnetEmission?>> { state.map { null } }
        } else {
            val reference = BittensorSubnetReference(subnet.id, registration)
            retryController.observe(
                step = "subnet emission",
                analytics = chainRpcReads,
                transform = { it.withSafeLoading() }
            ) {
                flowOf<BittensorSubnetEmission?> { subnetParametersInteractor.emission(reference) }
            }
        }
    }
        .shareInBackground()

    val website = subnet.map { it.websiteUrl?.let(BittensorSubnetLinks::websiteLabel) }
        .shareInBackground()

    val github = subnet.map { it.githubUrl?.let(BittensorSubnetLinks::githubLabel) }
        .shareInBackground()

    val parameters = combine(
        subnetState,
        assetState,
        emissionState
    ) { subnetState, assetState, emissionState ->
        val poolSides = bittensorLoadingValue(subnetState, assetState) {
            val subnet = requireNotNull(subnetState.dataOrNull)
            val asset = requireNotNull(assetState.dataOrNull)
            val nativeAmount = subnet.taoReserveInPlanks?.let(asset.token::amountFromPlanks)
            BittensorSubnetPoolSideUi(
                title = resources.getString(R.string.bittensor_token_in_pool, asset.token.configuration.symbol.value),
                amount = nativeAmount?.let { tokenFormatter.formatToken(it, asset.token.configuration.symbol, COMPACT_TOKEN_CONFIG) } ?: "—",
                fiat = nativeAmount?.let(asset.token::toFiatOrNull)?.let { fiatFormatter.formatFiat(it, asset.token.currency) }
            )
        }
        val alphaTitle = resources.getString(R.string.bittensor_alpha_in_pool)

        BittensorSubnetParametersUi(
            split = poolSides.map { native ->
                val subnet = requireNotNull(subnetState.dataOrNull)
                val share = bittensorPoolNativeShare(subnet.taoReserveInPlanks, subnet.alphaReserveInPlanks)
                BittensorSubnetPoolSplitUi(
                    nativeTitle = native.title,
                    alphaTitle = alphaTitle,
                    nativeShare = formatPoolShare(share),
                    alphaShare = formatPoolShare(share?.let { BigDecimal.ONE - it }),
                    nativeFraction = share?.toFloat()
                )
            },
            nativePool = poolSides,
            alphaPool = poolSides.map {
                val subnet = requireNotNull(subnetState.dataOrNull)
                val asset = requireNotNull(assetState.dataOrNull)
                val alphaAmount = subnet.alphaReserveInPlanks?.toBigDecimal(BITTENSOR_ALPHA_DECIMALS)
                val nativeValue = bittensorAlphaPoolValueInRao(subnet.alphaReserveInPlanks, subnet.priceInRao)
                    ?.let(asset.token::amountFromPlanks)
                BittensorSubnetPoolSideUi(
                    title = alphaTitle,
                    amount = alphaAmount?.let {
                        tokenFormatter.formatToken(it, TokenSymbol(subnet.symbol.ifBlank { "—" }), COMPACT_TOKEN_CONFIG).ltrIsolated()
                    } ?: "—",
                    fiat = nativeValue?.let(asset.token::toFiatOrNull)?.let { fiatFormatter.formatFiat(it, asset.token.currency) }
                )
            },
            emission = formatEmission(subnetState, emissionState)
        )
    }
        .shareInBackground()

    val price = combine(
        subnet,
        asset,
        isFiatMode,
        chartSelection
    ) { subnet, asset, fiat, selection ->
        if (selection != null) {
            return@combine if (fiat) {
                fiatFormatter.formatFiat(selection.point.price, asset.token.currency)
            } else {
                tokenFormatter.formatToken(selection.point.price, asset.token.configuration.symbol)
            }
        }
        val price = subnet.priceInRao?.let(asset.token::amountFromPlanks) ?: return@combine "—"
        if (fiat) {
            val fiatPrice = asset.token.toFiatOrNull(price) ?: return@combine "—"
            fiatFormatter.formatFiat(fiatPrice, asset.token.currency)
        } else {
            tokenFormatter.formatToken(price, asset.token.configuration.symbol)
        }
    }
        .shareInBackground()

    // Known before the pool loads, so the native rows are titled while their values still show progress
    val nativePoolTitle = asset.map { resources.getString(R.string.bittensor_token_in_pool, it.token.configuration.symbol.value) }
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
                analytics = priceServerReads,
                transform = { it.withSafeLoading() }
            ) {
                flowOf { loadChartHistory(request) }
            }
                .map { SubnetChartData(request.period, it) }
        }
        .shareInBackground()

    private val ageState = subnetState.flatMapLatest { state ->
        val subnet = state.dataOrNull
        if (subnet == null) {
            flowOf { state.map { null } }
        } else {
            retryController.observe(
                step = "subnet age",
                analytics = subnetsReads,
                transform = { it.withSafeLoading() }
            ) {
                flowOf { subnetSafetyInteractor.ageInDays(subnet) }
            }
        }
    }
        .shareInBackground()

    // Until the age is known the history is shown unclipped, as on iOS; it is re-clipped once the registration time arrives.
    private val registrationEpochSeconds = ageState.map { state ->
        state.dataOrNull?.let { days -> bittensorRegistrationEpochSeconds(days, System.currentTimeMillis() / 1000) }
    }
        .distinctUntilChanged()

    private val visibleChart = combine(chartData, registrationEpochSeconds) { data, registration -> data.clipped(registration) }
        .shareInBackground()

    val charts = combine(visibleChart, isFiatMode, ::mapCharts)
        .shareInBackground()

    val priceChangeFormatter = combine(visibleChart, period) { chart, selected ->
        val points = chart.points.dataOrNull.orEmpty()
        val covered = chart.period == selected && hasBittensorSubnetHistoryCoverage(points, selected)
        BittensorChartChangeTextInjector(periodLabel(selected), covered)
    }
        .shareInBackground()

    val age = ageState.mapLoading {
        it?.let(resources::formatBittensorSubnetAge) ?: "—"
    }
        .shareInBackground()

    private val selectedValidatorAddress = MutableStateFlow(initialValidatorAddress)

    private val validatorCandidates = selectedValidatorAddress.flatMapLatest { address ->
        retryController.observe(
            step = "subnet validator preview",
            analytics = validatorsReads,
            transform = { it.withSafeLoading() }
        ) {
            flowOf { if (address != null) validatorInteractor.previews(subnetId, address) else automaticValidatorPreviews() }
        }
    }
        .shareInBackground()

    private val validatorYields = retryController.observe(
        step = "subnet validator preview APY",
        analytics = validatorsReads,
        transform = { it.withSafeLoading() }
    ) {
        flowOf { validatorInteractor.yields(subnetId) }
    }
        .shareInBackground()

    private val validatorState = combine(validatorCandidates, validatorYields) { candidates, yields ->
        candidates.map { selectBittensorValidatorPreview(it, yields.dataOrNull.orEmpty()) }
    }
        .shareInBackground()

    val isValidatorLoading = validatorState.map { it.isLoading() }.shareInBackground()

    val availableValidators = combine(validatorState, validatorYields) { state, yields ->
        val validator = state.dataOrNull?.validator ?: return@combine emptyList()
        val chain = validatorInteractor.chain()
        val address = chain.addressOf(validator.hotkey.value)
        val rate = yields.dataOrNull?.get(validator.hotkey)
        listOf(
            BittensorPositionUi(
                id = address,
                title = validator.name.takeIf { it != address } ?: address.ellipsizeAddress(),
                subtitle = resources.getString(R.string.bittensor_validator),
                amount = AmountModel(rate?.formatAsPercentage() ?: "—", resources.getString(R.string.bittensor_estimated_apy)),
                icon = Icon.FromDrawable(addressIconGenerator.createAddressIcon(validator.hotkey.value, 32)),
                action = BittensorPositionAction.ValidatorDetails(address),
                iconSizeDp = 32,
                isAmountLoading = yields.isLoading(),
                amountColorRes = bittensorYieldColor(rate)
            )
        )
    }
        .shareInBackground()

    val canChooseValidator = combine(isValidatorLoading, availableValidators) { loading, rows ->
        !loading && rows.isEmpty()
    }
        .shareInBackground()

    init {
        analytics.track(BittensorAnalyticsEvent.SubnetDetailsOpened(subnetId.value, analyticsSource, AnalyticsPricePeriod.WEEK))
        validatorCommunicator.responseFlow
            .onEach { response ->
                if (!selectionState.accepts(response)) return@onEach
                selectionState = selectionState.withResponse(response)
                if (response.subnetId == subnetId.value && response.address != null) selectedValidatorAddress.value = response.address
            }
            .launchIn(this)
    }

    fun validatorClicked() = launchUnit {
        val validator = validatorState.first().dataOrNull?.validator
        val address = selectedValidatorAddress.value ?: validator?.let { validatorInteractor.chain().addressOf(it.hotkey.value) }
        val request = BittensorSelectValidatorCommunicator.Request(subnetId.value, address, selectionState)
        validatorCommunicator.openRequest(request)
    }

    private fun periodLabel(period: BittensorSubnetChartPeriod): String = resources.getString(period.labelRes())

    private fun formatPoolShare(share: BigDecimal?): String = share
        ?.movePointRight(2)
        ?.setScale(1, RoundingMode.HALF_UP)
        ?.formatAsPercentage()
        ?: "—"

    private fun formatEmission(
        subnetState: ExtendedLoadingState<BittensorSubnet>,
        emissionState: ExtendedLoadingState<BittensorSubnetEmission?>
    ): ExtendedLoadingState<CharSequence?> {
        val subnet = subnetState.dataOrNull
        val reference = subnet?.registrationBlock?.let { BittensorSubnetReference(subnet.id, it) }
        if (reference == null) return subnetState.map { "—" }
        val emission = emissionState.dataOrNull
        if (emission != null && emission.reference != reference) return ExtendedLoadingState.Loading
        return bittensorLoadingValue(subnetState, emissionState) {
            emission?.fraction?.formatFractionAsPercentage() ?: "—"
        }
    }

    fun showFiat(value: Boolean) {
        chartSelection.value = null
        _isFiatMode.value = value
    }

    fun selectChartPoint(selection: PriceChartSelection?) {
        chartSelection.value = selection
    }

    fun selectPeriod(index: Int) {
        chartSelection.value = null
        chartPeriods.getOrNull(index)
            ?.let { period.value = it }
    }

    fun toggleFavorite() = launchUnit {
        try {
            val favorites = bittensorSubnetInteractor.observeFavorites()
                .first()
            bittensorSubnetInteractor.toggleFavorite(favorites.metaId, subnet.first())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reportFailure("favorite", e)
        }
    }

    fun continueClicked() = launchUnit {
        if (!canContinue.first()) return@launchUnit
        val address = currentValidatorAddress()
        val isPreset = selectedValidatorAddress.value == null && address != null
        analytics.track(
            BittensorAnalyticsEvent.TypeSelected(
                BittensorAnalyticsEvent.NETWORK,
                StakingAnalyticsTarget.SUBNET,
                StakingAnalyticsSelectionMethod.SUBNET_DETAILS,
                subnetId.value
            )
        )
        router.openBittensorSubnetAmount(
            BittensorSubnetAmountPayload(
                subnetId.value,
                address,
                validatorIsPreset = isPreset,
                replaceAmount = replaceAmount,
                selectionState = selectionState
            )
        )
    }

    /** The same preset the stake setup applies (NW-2356), so the card here and the amount screen agree. */
    private suspend fun automaticValidatorPreviews(): List<BittensorValidatorPreview> {
        val preset = presetValidatorInteractor.preset(subnetId).preset
            ?: return validatorInteractor.previews(subnetId, null).map { it.copy(canPreselect = false) }
        return listOf(BittensorValidatorPreview(preset.validator, canPreselect = true))
    }

    private suspend fun currentValidatorAddress(): String? {
        selectedValidatorAddress.value?.let { return it }
        val preview = validatorState.first().dataOrNull?.takeIf { it.canPreselect } ?: return null
        return validatorInteractor.chain().addressOf(preview.validator.hotkey.value)
    }

    fun websiteClicked() = launchUnit {
        subnet.first().websiteUrl?.let(::showBrowser)
    }

    fun githubClicked() = launchUnit {
        subnet.first().githubUrl?.let(::showBrowser)
    }

    fun back() {
        validatorCommunicator.respond(BittensorSelectValidatorCommunicator.Response(null, null, selectionState))
        router.back()
    }

    /** Null when the subnet has no market listing, so no price history exists for any period. */
    private suspend fun loadChartHistory(request: BittensorSubnetChartRequest): List<BittensorSubnetPricePoint>? {
        // A missing native price id is a failed read (retryable), not a subnet without price history.
        val priceId = requireNotNull(request.nativePriceId) { "Native token price id is unavailable" }
        return bittensorSubnetDetailsInteractor.listedHistory(
            subnetId,
            priceId,
            request.currency,
            request.period
        )
    }

    private fun mapCharts(data: SubnetChartData, isFiatMode: Boolean): List<PriceChartModel> = chartPeriods.map { period ->
        mapChart(data, period, isFiatMode)
    }

    private fun mapChart(data: SubnetChartData, period: BittensorSubnetChartPeriod, isFiatMode: Boolean): PriceChartModel {
        val label = periodLabel(period)
        if (period != data.period) return PriceChartModel.Loading(label)

        return when (val state = data.points) {
            is ExtendedLoadingState.Loading -> PriceChartModel.Loading(label)
            is ExtendedLoadingState.Error -> PriceChartModel.Empty(label, resources.getString(R.string.bittensor_price_history_failed))
            is ExtendedLoadingState.Loaded -> {
                // Unlisted subnets and windows without points show the iOS empty state; the spot price still comes from the pool.
                if (state.data.isEmpty()) return PriceChartModel.Empty(label, unavailableChartMessage())

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

    private fun unavailableChartMessage(): String = resources.getString(R.string.bittensor_price_history_unavailable) + "\n" +
        resources.getString(R.string.bittensor_price_history_source)

    private fun reportFailure(
        step: String,
        cause: Throwable
    ) {
        logger.log("[BittensorSubnetDetails] Failed to load $step")
        logger.log(cause)
        showError(resources.getString(R.string.common_retry_message))
    }
}

/** [points] holds null while the subnet is known to have no market listing. */
private class SubnetChartData(
    val period: BittensorSubnetChartPeriod,
    private val history: ExtendedLoadingState<List<BittensorSubnetPricePoint>?>,
    private val registrationEpochSeconds: Long? = null
) {

    val points: ExtendedLoadingState<List<BittensorSubnetPricePoint>> = history.map { clipBittensorSubnetHistory(it.orEmpty(), registrationEpochSeconds) }

    fun clipped(registrationEpochSeconds: Long?) = SubnetChartData(period, history, registrationEpochSeconds)
}

// Pool totals are compact, as on iOS: "8.4K TAO", "1.54M ψ"
private val COMPACT_TOKEN_CONFIG = TokenConfig(useCompactAbbreviation = true)
