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
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.runtime.ext.accountIdOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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
    private val abandonment: BittensorChooseSubnetAbandonment,
    private val replaceAmount: Boolean,
    initialValidatorAddress: String?,
    initialSelectionState: BittensorSelectionState = BittensorSelectionState(),
    private val analyticsSource: SubnetDetailsSource = SubnetDetailsSource.PICKER
) : BaseViewModel(), Browserable.Presentation by Browserable() {

    private var selectionState = initialSelectionState

    private var completesOnValidatorSelection = false

    // Only details opened from the start picker share its abandonment, as iOS hands them the picker's tracker.
    private val tracksAbandonment = analyticsSource == SubnetDetailsSource.PICKER && !replaceAmount

    private val chartSelection = MutableStateFlow<PriceChartSelection?>(null)

    val chartDateFormatter = RealDateChartTextInjector(resources)

    private val subnetsReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.SUBNETS, BittensorScreen.SUBNET_DETAILS)
    private val priceServerReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.PRICE_SERVER, BittensorScreen.SUBNET_DETAILS)

    // Only the subnet and the wallet asset block the screen and offer a retry dialog; the other reads fail inline, as on iOS.
    val retryEvents = retryController.events

    private val appActivations = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val chartRetries = MutableStateFlow(0)

    // As on iOS, a subnet without a market listing stays unlisted for this screen: no period or currency change loads it again.
    private val isUnlisted = AtomicBoolean(false)

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

    val validatorCount = quietRead("subnet validator count") {
        bittensorSubnetStakingInteractor.validatorCount(subnetId).toString()
    }
        .shareInBackground()

    private val emissionState = subnetState.flatMapLatest { state ->
        val subnet = state.dataOrNull
        val registration = subnet?.registrationBlock
        if (subnet == null || registration == null) {
            flowOf<ExtendedLoadingState<BittensorSubnetEmission?>> { state.map { null } }
        } else {
            val reference = BittensorSubnetReference(subnet.id, registration)
            quietRead<BittensorSubnetEmission?>("subnet emission") { subnetParametersInteractor.emission(reference) }
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
                title = resources.getString(R.string.bittensor_token_pool, asset.token.configuration.symbol.value),
                amount = nativeAmount?.let { tokenFormatter.formatToken(it, asset.token.configuration.symbol) } ?: "—",
                fiat = nativeAmount?.let(asset.token::toFiatOrNull)?.let { fiatFormatter.formatFiat(it, asset.token.currency) }
            )
        }
        val alphaTitle = subnetState.dataOrNull?.symbol?.takeIf { it.isNotBlank() }
            ?.let { resources.getString(R.string.bittensor_token_pool, it) }
            ?: resources.getString(R.string.bittensor_alpha_pool)

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
                    amount = alphaAmount?.let { tokenFormatter.formatToken(it, TokenSymbol(subnet.symbol.ifBlank { "—" })) } ?: "—",
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
        .distinctUntilChanged()
        .combine(chartRetries) { request, _ -> request }
        .flatMapLatest { request ->
            chartHistory(request).map { SubnetChartData(request.period, it) }
        }
        .shareInBackground()

    private val ageState = subnetState.flatMapLatest { state ->
        val subnet = state.dataOrNull
        if (subnet == null) {
            flowOf { state.map { null } }
        } else {
            quietRead("subnet age") { subnetSafetyInteractor.age(subnet) }
        }
    }
        .shareInBackground()

    // Until the age is known the history is shown unclipped, as on iOS; it is re-clipped once the registration time arrives.
    private val registrationEpochSeconds = ageState.map { state ->
        state.dataOrNull?.let { age -> bittensorRegistrationEpochSeconds(age, System.currentTimeMillis()) }
    }
        .distinctUntilChanged()

    private val visibleChart = combine(chartData, registrationEpochSeconds) { data, registration -> data.clipped(registration) }
        .shareInBackground()

    val charts = combine(visibleChart, isFiatMode, ::mapCharts)
        .shareInBackground()

    val isChartFailed = combine(visibleChart, period) { chart, selected ->
        chart.period == selected && chart.points is ExtendedLoadingState.Error
    }
        .distinctUntilChanged()
        .shareInBackground()

    val priceChangeFormatter = combine(visibleChart, period) { chart, selected ->
        val points = chart.points.dataOrNull.orEmpty()
        val covered = chart.period == selected && hasBittensorSubnetHistoryCoverage(points, selected)
        BittensorChartChangeTextInjector(periodLabel(selected), covered)
    }
        .shareInBackground()

    val age = ageState.mapLoading {
        it?.let { age ->
            resources.getString(
                R.string.bittensor_safety_age_days,
                formatAgeInDays(age)
            )
        } ?: "—"
    }
        .shareInBackground()

    private val selectedValidatorAddress = MutableStateFlow(initialValidatorAddress)

    private val validatorCandidates = selectedValidatorAddress.flatMapLatest { address ->
        if (address != null) {
            flowOf { selectedValidatorPreviews(address) }.withSafeLoading()
        } else {
            quietRead("subnet validator preset") { automaticValidatorPreviews() }
        }
    }
        .shareInBackground()

    // As on iOS, the rate is resolved once: a failure shows "—" and is not requested again.
    private val validatorYields = quietRead("subnet validator preview APY", requestAgainOnForeground = false) {
        validatorInteractor.yields(subnetId)
    }
        .shareInBackground()

    private val validatorState = combine(validatorCandidates, validatorYields) { candidates, yields ->
        candidates.map { selectBittensorValidatorPreview(it, yields.dataOrNull.orEmpty()) }
    }
        .shareInBackground()

    val isValidatorLoading = validatorState.map { it.isLoading() }.shareInBackground()

    // As on iOS, "Use this subnet" waits for the automatic preset; a validator the user chose is usable at once.
    val canContinue = combine(subnetState, selectedValidatorAddress, validatorState) { subnet, address, validator ->
        subnet.isLoaded() && (address != null || !validator.isLoading())
    }
        .shareInBackground()

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
                val completes = completesOnValidatorSelection
                completesOnValidatorSelection = false
                val address = response.address
                if (response.subnetId == subnetId.value && address != null) {
                    selectedValidatorAddress.value = address
                    if (completes) openAmount(address, isPreset = false)
                }
            }
            .launchIn(this)
    }

    fun validatorClicked() = launchUnit {
        val validator = validatorState.first().dataOrNull?.validator
        val address = selectedValidatorAddress.value ?: validator?.let { validatorInteractor.chain().addressOf(it.hotkey.value) }
        openValidatorSelection(address, completesOnSelection = false)
    }

    private fun openValidatorSelection(address: String?, completesOnSelection: Boolean) {
        completesOnValidatorSelection = completesOnSelection
        validatorCommunicator.openRequest(BittensorSelectValidatorCommunicator.Request(subnetId.value, address, selectionState))
    }

    private fun periodLabel(period: BittensorSubnetChartPeriod): String = resources.getString(period.labelRes())

    private fun formatAgeInDays(age: Duration): String = age.inWholeMilliseconds.toBigDecimal()
        .divide(1.days.inWholeMilliseconds.toBigDecimal(), 0, RoundingMode.HALF_EVEN)
        .toPlainString()

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

    fun retryChart() {
        // As on iOS, only a failed history offers Try again.
        if (visibleChart.replayCache.lastOrNull()?.points !is ExtendedLoadingState.Error) return
        priceServerReads.retry()
        chartRetries.value += 1
    }

    /** Like iOS didBecomeActive: reads that failed quietly are requested again when the app returns to the foreground. */
    fun appBecameActive() {
        appActivations.tryEmit(Unit)
    }

    fun becomeActive() {
        if (tracksAbandonment) abandonment.becomeActive(selectionState.flowId, BittensorScreen.SUBNET_DETAILS, StakingAnalyticsTarget.SUBNET)
    }

    /** The period the chart shows; the view switches a chip on tap, before [selectPeriod] may ignore the switch. */
    val selectedPeriodIndex: Int
        get() = chartPeriods.indexOf(period.value)

    fun selectPeriod(index: Int) {
        if (isUnlisted.get()) return
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
        if (address == null) {
            // As on iOS, without a validator the list opens first and the stake continues once one is chosen there.
            openValidatorSelection(null, completesOnSelection = true)
            return@launchUnit
        }
        openAmount(address, isPreset = selectedValidatorAddress.value == null)
    }

    private fun openAmount(address: String, isPreset: Boolean) {
        if (tracksAbandonment) abandonment.markProceeded(selectionState.flowId)
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

    /**
     * The same preset the stake setup applies (NW-2356), so the card here and the amount screen agree.
     * Without one the card asks to choose a validator, as on iOS.
     */
    private suspend fun automaticValidatorPreviews(): List<BittensorValidatorPreview> {
        val preset = presetValidatorInteractor.preset(subnetId).preset ?: return emptyList()
        return listOf(BittensorValidatorPreview(preset.validator, canPreselect = true))
    }

    /** A chosen validator stays on the card when its name cannot be loaded, as iOS shows the validator it was handed. */
    private suspend fun selectedValidatorPreviews(address: String): List<BittensorValidatorPreview> {
        attempt("subnet validator name") { validatorInteractor.previews(subnetId, address) }.dataOrNull?.let { return it }
        val hotkey = AccountIdKey(validatorInteractor.chain().accountIdOf(address))
        return listOf(BittensorValidatorPreview(BittensorValidator(hotkey, address), canPreselect = true))
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

    private fun chartHistory(request: BittensorSubnetChartRequest): Flow<ExtendedLoadingState<List<BittensorSubnetPricePoint>?>> = flow {
        if (isUnlisted.get()) {
            emit(ExtendedLoadingState.Loaded(null))
            return@flow
        }
        emit(ExtendedLoadingState.Loading)
        val state = attempt("subnet chart") { loadChartHistory(request) }
        when (state) {
            is ExtendedLoadingState.Error -> priceServerReads.failed(state.exception)
            is ExtendedLoadingState.Loaded -> if (state.data == null) isUnlisted.set(true)
            ExtendedLoadingState.Loading -> Unit
        }
        emit(state)
    }

    private fun mapCharts(data: SubnetChartData, isFiatMode: Boolean): List<PriceChartModel> = chartPeriods.map { period ->
        mapChart(data, period, isFiatMode)
    }

    private fun mapChart(data: SubnetChartData, period: BittensorSubnetChartPeriod, isFiatMode: Boolean): PriceChartModel {
        val label = periodLabel(period)
        if (period != data.period) return PriceChartModel.Loading(label)

        return when (val state = data.points) {
            is ExtendedLoadingState.Loading -> PriceChartModel.Loading(label)
            // The screen draws the failure with its Try again action over the empty plot.
            is ExtendedLoadingState.Error -> PriceChartModel.Empty(label, "")
            is ExtendedLoadingState.Loaded -> {
                // Unlisted subnets and windows with fewer than two points show the iOS empty state; the spot price still comes from the pool.
                if (state.data.size < 2) return PriceChartModel.Empty(label, unavailableChartMessage())

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

    /**
     * A read that does not block the screen: as on iOS its failure raises no dialog and, when [requestAgainOnForeground],
     * the read is requested again once the app returns to the foreground.
     */
    private fun <T> quietRead(
        step: String,
        requestAgainOnForeground: Boolean = true,
        load: suspend () -> T
    ): Flow<ExtendedLoadingState<T>> = flow {
        emit(ExtendedLoadingState.Loading)
        var state = attempt(step, load)
        emit(state)
        while (requestAgainOnForeground && state is ExtendedLoadingState.Error) {
            appActivations.first()
            state = attempt(step, load)
            emit(state)
        }
    }

    private suspend fun <T> attempt(step: String, load: suspend () -> T): ExtendedLoadingState<T> = try {
        ExtendedLoadingState.Loaded(load())
    } catch (cause: Exception) {
        // A child request timing out is a failed read, not cancellation of this screen.
        currentCoroutineContext().ensureActive()
        if (cause is CancellationException && cause !is TimeoutCancellationException) throw cause
        logger.log("[BittensorSubnetDetails] Failed to load $step")
        logger.log(cause)
        ExtendedLoadingState.Error(cause)
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

/** [points] holds null while the subnet is known to have no market listing. */
private class SubnetChartData(
    val period: BittensorSubnetChartPeriod,
    private val history: ExtendedLoadingState<List<BittensorSubnetPricePoint>?>,
    private val registrationEpochSeconds: Long? = null
) {

    val points: ExtendedLoadingState<List<BittensorSubnetPricePoint>> = history.map { clipBittensorSubnetHistory(it.orEmpty(), registrationEpochSeconds) }

    fun clipped(registrationEpochSeconds: Long?) = SubnetChartData(period, history, registrationEpochSeconds)
}
