package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorLoadingValue
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.presentation.fallbackIcon
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.common.utils.images.asUrlIcon
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorEstimatesMixin
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimateContext
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFeeEstimate
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorSlippageSettings
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.forRequest
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorExitValidationFailureUi
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.asFeeContextFromSelf
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefaultBy
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountInputModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountModel
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private data class ExitAmountInput(val text: String, val revision: Long = 0)

private data class ExitPreparationRequest(
    val position: BittensorExitPosition?,
    val input: ExitAmountInput,
    val slippageFraction: BigDecimal
)

private data class ExitAmountRequest(val preparation: ExitPreparationRequest, val exit: BittensorExitRequest, val estimateOnly: Boolean)

class BittensorExitAmountViewModel(
    private val payload: BittensorExitPositionPayload,
    private val bittensorExitInteractor: BittensorExitInteractor,
    private val router: StakingRouter,
    private val resources: ResourceManager,
    private val assetIconProvider: AssetIconProvider,
    private val addressIconGenerator: AddressIconGenerator,
    private val tokenFormatter: TokenFormatter,
    private val fiatFormatter: FiatFormatter,
    private val externalActions: ExternalActions.Presentation,
    estimatesMixin: BittensorEstimatesMixin,
    private val retryController: BittensorRetryController,
    private val validationExecutor: ValidationExecutor,
    private val exitValidationSystem: BittensorExitValidationSystem,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    private val exitFormatter: BittensorExitFormatter,
    private val slippageSettings: BittensorSlippageSettings,
    private val slippageSettingsCommunicator: SlippageSettingsCommunicator,
    private val costBasisFormatter: BittensorSaleCostBasisFormatter
) : BaseViewModel(), ExternalActions by externalActions, Validatable by validationExecutor {

    val isRoot = payload.netuid().isRoot()

    private companion object {

        const val AMOUNT_DEBOUNCE_MILLIS = 350L
    }

    val retryEvents = retryController.events

    private val validating = MutableStateFlow(false)

    private val input = MutableStateFlow(ExitAmountInput(""))

    private val selectedSlippage = if (isRoot) {
        flowOf { bittensorExitInteractor.slippage.fraction }
    } else {
        slippageSettings.observe()
    }.shareInBackground()

    val amount = input.map { it.text }
        .shareInBackground()

    private val positionState = retryController.observe(
        step = "exit position",
        transform = { it.withSafeLoading() }
    ) {
        flowOf { bittensorExitInteractor.load(payload.metaId, payload.netuid(), payload.hotkeyAddress) }
    }
        .shareInBackground()

    val position = positionState.map { it.dataOrNull }
        .shareInBackground()

    private val requestState = combine(
        position,
        input,
        selectedSlippage
    ) { position, input, slippage -> ExitPreparationRequest(position, input, slippage) }
        .flatMapLatest { request ->
            retryController.observe(
                step = "exit quote",
                transform = { it.withSafeLoading() }
            ) {
                flowOf {
                    delay(AMOUNT_DEBOUNCE_MILLIS)
                    prepareRequest(request)
                }
            }
        }
        .shareInBackground()

    private val request = requestState.map { it.dataOrNull?.exit }
        .shareInBackground()

    private val feeContext = request.mapNotNull { it?.position?.asset?.token?.configuration }
        .asFeeContextFromSelf()

    val feeLoaderMixin = feeLoaderMixinFactory.createDefaultBy<BittensorFeeEstimate<BittensorExitRequest>>(
        scope = this,
        feeContext = feeContext
    )

    val feeStatus = combine(
        request,
        feeLoaderMixin.fee
    ) { request, status -> status.forRequest(request) }
        .shareInBackground()

    private val prepared = combine(
        requestState,
        feeStatus
    ) { state, status ->
        val request = state.dataOrNull?.takeUnless { it.estimateOnly }?.exit
        val estimate = status.loadedFeeOrNull()
        when {
            request == null -> state.map { null }
            status is FeeStatus.Error || status is FeeStatus.NoFee -> ExtendedLoadingState.Loaded(null)
            estimate == null || estimate.request != request -> ExtendedLoadingState.Loading
            else -> {
                val fromStake = bittensorExitInteractor.feeFromStake(request.position, estimate)
                val draft = bittensorExitInteractor.prepare(
                    request.position,
                    request.quote,
                    estimate,
                    fromStake
                )
                ExtendedLoadingState.Loaded(draft)
            }
        }
    }
        .shareInBackground()

    val title = position.map {
        if (isRoot) {
            resources.getString(R.string.bittensor_exit_root_title)
        } else {
            resources.getString(
                R.string.bittensor_exit_sell_title,
                it?.subnet?.name?.ifBlank { resources.getString(R.string.bittensor_subnet_number, payload.netuid) } ?: resources.getString(
                    R.string.bittensor_subnet_number,
                    payload.netuid
                )
            )
        }
    }
        .shareInBackground()

    val amountModel = position.map { position ->
        position?.let {
            val icon = if (isRoot) {
                assetIconProvider.getAssetIconOrFallback(it.asset.token.configuration)
            } else {
                it.subnet?.logoUrl?.asUrlIcon() ?: AssetIconProvider.fallbackIcon
            }
            val symbol = if (isRoot) {
                it.asset.token.configuration.symbol.value
            } else {
                it.subnet?.name?.ifBlank { it.subnet?.symbol.orEmpty() } ?: resources.getString(R.string.bittensor_subnet_number, payload.netuid)
            }
            ChooseAmountModel(ChooseAmountInputModel(symbol, icon))
        }
    }
        .shareInBackground()

    val maxAmount = combine(position, selectedSlippage) { position, slippage ->
        position?.let { formatInput(it, bittensorExitInteractor.maximumAmount(it, slippage)) }
    }
        .shareInBackground()

    val fiatAmount = combine(
        position,
        amount
    ) { position, text ->
        position ?: return@combine null
        val input = text.toBigDecimalOrNull() ?: return@combine null
        val tao = if (isRoot) input else position.subnet?.priceInRao?.let { input * position.asset.token.amountFromPlanks(it) } ?: return@combine null
        val fiat = position.asset.token.toFiatOrNull(tao) ?: return@combine null
        fiatFormatter.formatFiat(fiat, position.asset.token.currency)
    }
        .shareInBackground()

    private val costBasisState = estimatesMixin.observeCostBasis(
        position.map {
            it?.takeUnless { it.isRoot }
                ?.let { BittensorEstimateContext(it.metaId, it.chain) }
        }
    )
        .shareInBackground()

    val averagePrice = combine(
        costBasisState,
        position
    ) { state, position ->
        state.map { basis ->
            costBasisFormatter.averagePrice(basis, position, payload.netuid)
        }
    }
        .shareInBackground()

    val averagePriceDescription = position.map { position ->
        position?.subnet?.let { resources.getString(R.string.bittensor_average_price_description, it.symbol, it.name) }
    }
        .shareInBackground()

    val earned = combine(
        costBasisState,
        prepared,
        requestState
    ) { state, prepared, request ->
        if (request.dataOrNull?.estimateOnly == true) return@combine ExtendedLoadingState.Loaded(costBasisFormatter.profit(null, null))
        bittensorLoadingValue(state, prepared) {
            costBasisFormatter.profit(state.dataOrNull, prepared.dataOrNull, payload.netuid)
        }
    }
        .shareInBackground()

    val validator = payload.hotkeyAddress

    val validatorIcon = position.map { position ->
        position?.let { addressIconGenerator.createAddressIcon(it.hotkey.value, 18, AddressIconGenerator.BACKGROUND_TRANSPARENT) }
    }
        .shareInBackground()

    val received = prepared.mapLoading {
        it?.let { draft ->
            val received = draft.netReceive
            "≈ " + tokenFormatter.formatToken(draft.position.asset.token.amountFromPlanks(received), draft.position.asset.token.configuration.symbol)
        } ?: "—"
    }
        .shareInBackground()

    val rate = requestState.mapLoading { state -> state?.exit?.let { exitFormatter.exchangeRate(it.position, it.quote) } ?: "—" }
        .shareInBackground()

    val fee = combine(positionState, requestState, feeStatus) { position, quote, status ->
        val estimate = status.loadedFeeOrNull()
        when {
            position.isLoading() || quote.isLoading() || status is FeeStatus.Loading -> ExtendedLoadingState.Loading
            else -> ExtendedLoadingState.Loaded(
                estimate?.let { tokenFormatter.formatToken(it.decimalAmount, it.asset.symbol).toString() } ?: "—"
            )
        }
    }
        .shareInBackground()

    val feeCaption = prepared.map { state ->
        val draft = state.dataOrNull ?: return@map null
        val fromStake = draft.feeFromStake
        resources.getString(
            if (fromStake) R.string.bittensor_exit_fee_from_stake else R.string.bittensor_exit_fee_from_free,
            draft.position.asset.token.configuration.symbol.value
        )
    }
        .shareInBackground()

    val saleFeeWarning = combine(request, feeStatus) { request, status ->
        if (isRoot || request == null) return@combine null
        val estimate = status.loadedFeeOrNull()?.takeIf { it.request == request } ?: return@combine null
        val fee = tokenFormatter.formatToken(estimate.decimalAmount, estimate.asset.symbol, TokenConfig(roundingMode = RoundingMode.UP))
        val quote = request.quote
        val fromSale = bittensorExitInteractor.feeFromStake(request.position, estimate) && quote.plan?.isBatched != true && quote.exitHotkeys.size <= 1
        resources.getString(if (fromSale) R.string.bittensor_sell_fee_from_sale else R.string.bittensor_sell_fee_from_free, fee)
    }
        .shareInBackground()

    val continueButtonState = combine(
        positionState,
        prepared,
        validating
    ) { position, prepared, validating ->
        val label = resources.getString(R.string.common_continue)
        when {
            validating || position.isLoading() || prepared.isLoading() -> DescriptiveButtonState.Loading
            prepared.dataOrNull == null -> DescriptiveButtonState.Disabled(label)
            else -> DescriptiveButtonState.Enabled(label)
        }
    }
        .shareInBackground()

    init {
        if (!isRoot) {
            slippageSettingsCommunicator.responseFlow
                .onEach { slippageSettings.set(BigDecimal.valueOf(it.fraction)) }
                .launchIn(this)
        }

        feeLoaderMixin.connectWith(request) { _, request ->
            request?.let {
                val fee = bittensorExitInteractor.estimateFee(it.position, it.quote)
                BittensorFeeEstimate(it, fee)
            }
        }
    }

    fun amountChanged(value: String) {
        if (input.value.text != value) input.value = ExitAmountInput(value)
    }

    fun openSlippageSettings() {
        if (!isRoot) slippageSettingsCommunicator.openRequest(slippageSettings.payload())
    }

    fun maxClicked() = launchUnit {
        val position = position.first() ?: return@launchUnit
        val max = bittensorExitInteractor.maximumAmount(position, currentSlippage())
        input.value = ExitAmountInput(
            BigDecimal(max, position.amountPrecision).toStripTrailingZerosString()
        )
    }

    fun continueClicked(): Unit = continueWithImpact(null)

    private fun continueWithImpact(acceptedQuote: BittensorExitQuote?): Unit = launchUnit {
        if (validating.value || continueButtonState.first() !is DescriptiveButtonState.Enabled) return@launchUnit
        val draft = prepared.first().dataOrNull ?: return@launchUnit
        val quoted = requestState.first().dataOrNull ?: return@launchUnit
        if (quoted.exit != BittensorExitRequest(draft.position, draft.quote) || !isCurrent(quoted)) return@launchUnit
        val current = quoted.preparation.input
        val currentAmount = runCatching {
            current.text.toBigDecimalOrNull()
                ?.movePointRight(draft.position.amountPrecision)
                ?.toBigIntegerExact()
        }
            .getOrNull()
        if (currentAmount != draft.quote.amount) return@launchUnit

        validating.value = true
        try {
            val freshPayload = bittensorExitInteractor.validationPayload(draft.position, draft.quote, draft.fee)
            val validationPayload = freshPayload.copy(priceImpactAccepted = acceptedQuote != null && freshPayload.latestQuote == acceptedQuote)
            validationExecutor.requireValid(
                validationSystem = exitValidationSystem,
                payload = validationPayload,
                validationFailureTransformerCustom = { failure, _ ->
                    bittensorExitValidationFailureUi(
                        failure.reason,
                        resources,
                        unstakeAll = { withCurrentRequest(quoted) { maxClicked() } },
                        refreshQuote = { withCurrentRequest(quoted) { input.value = current.copy(revision = current.revision + 1) } },
                        acceptImpact = { withCurrentRequest(quoted) { continueWithImpact(validationPayload.latestQuote) } }
                    )
                }
            ) {
                withCurrentRequest(quoted) { router.openBittensorExitReview(draft.quote.toPayload(payload)) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showError(e)
        } finally {
            validating.value = false
        }
    }

    fun validatorClicked() = launchUnit {
        val position = position.first() ?: return@launchUnit
        externalActions.showAddressActions(payload.hotkeyAddress, position.chain)
    }

    fun back() = router.back()

    private fun currentSlippage(): BigDecimal = if (isRoot) bittensorExitInteractor.slippage.fraction else slippageSettings.get()

    private suspend fun isCurrent(quoted: ExitAmountRequest): Boolean {
        val current = ExitPreparationRequest(position.first(), input.value, currentSlippage())
        return quoted.preparation == current && requestState.first().dataOrNull == quoted
    }

    private fun withCurrentRequest(quoted: ExitAmountRequest, action: () -> Unit): Unit = launchUnit {
        if (isCurrent(quoted)) action()
    }

    private suspend fun prepareRequest(request: ExitPreparationRequest): ExitAmountRequest? {
        val position = request.position ?: return null
        val entered = request.input.text.toBigDecimalOrNull()?.takeIf { it.isPositive }
        val estimateOnly = entered == null
        val maximum = bittensorExitInteractor.maximumAmount(position, request.slippageFraction)
        val value = entered ?: minOf(BigDecimal.ONE, BigDecimal(maximum, position.amountPrecision))
        val amount = runCatching {
            value.movePointRight(position.amountPrecision).toBigIntegerExact()
        }
            .getOrNull() ?: return null
        if (amount <= BigInteger.ZERO || amount.bitLength() > 64) return null
        if (amount > maximum) return null
        val fullExit = position.preflight.canExitAll && amount == position.preflight.totalStake
        val quote = bittensorExitInteractor.quote(position, amount, fullExit, request.slippageFraction)
        return ExitAmountRequest(request, BittensorExitRequest(position, quote), estimateOnly)
    }

    private fun formatInput(position: BittensorExitPosition, amount: BigInteger): String = tokenFormatter.formatToken(
        BigDecimal(amount, position.amountPrecision),
        if (isRoot) position.asset.token.configuration.symbol else TokenSymbol(position.subnet?.symbol?.ifBlank { "—" } ?: "—")
    ).toString()
}
