package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitDraft
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import kotlinx.coroutines.delay
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimateContext
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorEstimatesRepository
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.presentation.fallbackIcon
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.common.utils.images.asUrlIcon
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitPosition
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicSubmissionRecovery
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicRecoveryState
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountInputModel
import io.novafoundation.nova.runtime.ext.ChainGeneses
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.math.BigInteger

private enum class ExitAmountMode { CUSTOM, MAX }

private data class ExitAmountInput(val text: String, val mode: ExitAmountMode = ExitAmountMode.CUSTOM)

private data class ExitPreparationRequest(val position: BittensorExitPosition?, val input: ExitAmountInput)

class BittensorExitAmountViewModel(
    private val payload: BittensorExitPositionPayload,
    private val interactor: BittensorExitInteractor,
    private val router: StakingRouter,
    private val resources: ResourceManager,
    private val icons: AssetIconProvider,
    private val addresses: AddressIconGenerator,
    private val tokens: TokenFormatter,
    private val fiatFormatter: FiatFormatter,
    recovery: ExtrinsicSubmissionRecovery,
    private val externalActions: ExternalActions.Presentation,
    estimates: BittensorEstimatesRepository
) : BaseViewModel(), ExternalActions by externalActions {

    val isRoot = payload.netuid().isRoot()

    private companion object {

        const val AMOUNT_DEBOUNCE_MILLIS = 350L
    }

    private val reload = MutableStateFlow(0L)

    private val input = MutableStateFlow(ExitAmountInput(""))

    val amount = input.map { it.text }.shareInBackground()

    private val positionState = reload.flatMapLatest {
        flowOf { interactor.load(payload.metaId, payload.netuid(), payload.hotkeyAddress) }.withSafeLoading()
    }.shareInBackground()

    val position = positionState.map { it.dataOrNull }.shareInBackground()

    private val prepared = combine(position, input, reload) { position, input, _ -> ExitPreparationRequest(position, input) }
        .flatMapLatest { request ->
            flowOf {
                delay(AMOUNT_DEBOUNCE_MILLIS)
                prepare(request)
            }.withSafeLoading()
        }.shareInBackground()

    private val pending = recovery.observe(payload.metaId, ChainGeneses.BITTENSOR).shareInBackground()

    val error = combine(positionState, prepared, pending) { position, prepared, pending ->
        when {
            pending is ExtrinsicRecoveryState.Pending -> resources.getString(R.string.bittensor_submission_unknown_description)
            position is ExtendedLoadingState.Error -> position.exception.message
            prepared is ExtendedLoadingState.Error -> prepared.exception.message
            else -> null
        }
    }.shareInBackground()

    val title = position.map {
        if (isRoot) {
            resources.getString(R.string.bittensor_exit_root_title)
        } else {
            resources.getString(R.string.bittensor_exit_sell_title, it?.subnet?.name?.ifBlank { "Subnet ${payload.netuid}" } ?: "Subnet ${payload.netuid}")
        }
    }.shareInBackground()

    val amountModel = position.map { position ->
        position?.let {
            val icon = if (isRoot) {
                icons.getAssetIconOrFallback(it.asset.token.configuration)
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
    }.shareInBackground()

    val maxAmount = position.map { it?.let { formatInput(it, it.preflight.stake.min(it.preflight.availability.available)) } }.shareInBackground()

    val fiatAmount = combine(position, amount) { position, text ->
        position ?: return@combine null
        val input = text.toBigDecimalOrNull() ?: return@combine null
        val tao = if (isRoot) input else position.subnet?.priceInRao?.let { input * position.asset.token.amountFromPlanks(it) } ?: return@combine null
        val fiat = position.asset.token.toFiatOrNull(tao) ?: return@combine null
        fiatFormatter.formatFiat(fiat, position.asset.token.currency)
    }.shareInBackground()

    private val costBasisState = estimates.observeCostBasis(
        position.map { it?.takeUnless { it.isRoot }?.let { BittensorEstimateContext(it.metaId, it.chain) } },
        reload
    ).shareInBackground()

    val averagePrice = combine(costBasisState, position) { state, position ->
        state.map { basis ->
            val average = basis?.forSubnet(payload.netuid)?.averagePrice
            if (average == null || position == null) "—" else "≈ " + tokens.formatToken(average, position.asset.token.configuration.symbol)
        }
    }.shareInBackground()

    val earned = combine(costBasisState, prepared) { state, prepared ->
        state.map { basis ->
            val draft = prepared.dataOrNull
            val totals = basis?.forSubnet(payload.netuid)
            if (draft == null || totals == null) {
                "—"
            } else {
                val profit = totals.estimatedProfit(draft.quote.amount, draft.quote.estimatedTaoOut - draft.quote.serviceFee)
                "≈ " + tokens.formatToken(draft.position.asset.token.amountFromPlanks(profit), draft.position.asset.token.configuration.symbol)
            }
        }
    }.shareInBackground()

    val validator = payload.hotkeyAddress

    val validatorIcon = position.map { position ->
        position?.let { addresses.createAddressIcon(it.hotkey.value, 18, AddressIconGenerator.BACKGROUND_TRANSPARENT) }
    }.shareInBackground()

    val received = prepared.map {
        it.dataOrNull?.let { draft ->
            val received = draft.quote.estimatedTaoOut - draft.quote.serviceFee
            "≈ " + tokens.formatToken(draft.position.asset.token.amountFromPlanks(received), draft.position.asset.token.configuration.symbol)
        } ?: "—"
    }.shareInBackground()

    val rate = prepared.map {
        it.dataOrNull?.let { draft ->
            val ratio = draft.quote.estimatedTaoOut.toBigDecimal().divide(draft.quote.amount.toBigDecimal(), 9, java.math.RoundingMode.DOWN)
            "1 ${draft.position.subnet?.symbol?.ifBlank { "—" } ?: "—"} ≈ " + tokens.formatToken(ratio, draft.position.asset.token.configuration.symbol)
        } ?: "—"
    }.shareInBackground()

    val fee = prepared.map {
        it.dataOrNull?.let { draft ->
            tokens.formatToken(draft.fee.decimalAmount, draft.fee.asset.symbol).toString()
        } ?: "—"
    }.shareInBackground()

    val feeCaption = prepared.map { state ->
        val draft = state.dataOrNull ?: return@map null
        val fromStake = draft.feeFromStake
        resources.getString(
            if (fromStake) R.string.bittensor_exit_fee_from_stake else R.string.bittensor_exit_fee_from_free,
            draft.position.asset.token.configuration.symbol.value
        )
    }.shareInBackground()

    val continueButtonState = combine(positionState, prepared, pending) { position, prepared, pending ->
        val label = resources.getString(R.string.common_continue)
        when {
            position.isLoading() || prepared.isLoading() -> DescriptiveButtonState.Loading
            prepared.dataOrNull == null || pending is ExtrinsicRecoveryState.Pending -> DescriptiveButtonState.Disabled(label)
            else -> DescriptiveButtonState.Enabled(label)
        }
    }.shareInBackground()

    fun amountChanged(value: String) {
        if (input.value.text != value) input.value = ExitAmountInput(value)
    }

    fun maxClicked() {
        val position = position.replayCache.lastOrNull() ?: return
        val max = position.preflight.stake.min(position.preflight.availability.available)
        val mode = if (max == position.preflight.stake) ExitAmountMode.MAX else ExitAmountMode.CUSTOM
        input.value = ExitAmountInput(BigDecimal(max, position.amountPrecision).toStripTrailingZerosString(), mode)
    }

    fun retry() = reload.update { it + 1 }

    fun continueClicked() {
        val draft = prepared.replayCache.lastOrNull()?.dataOrNull ?: return
        val current = input.value
        val currentAmount = runCatching { current.text.toBigDecimalOrNull()?.movePointRight(draft.position.amountPrecision)?.toBigIntegerExact() }.getOrNull()
        if (currentAmount != draft.quote.amount || (current.mode == ExitAmountMode.MAX) != draft.quote.fullExit) return
        if (continueButtonState.replayCache.lastOrNull() is DescriptiveButtonState.Enabled) {
            router.openBittensorExitReview(draft.quote.toPayload(payload))
        }
    }

    fun validatorClicked() = launchUnit {
        val position = position.replayCache.lastOrNull() ?: return@launchUnit
        externalActions.showAddressActions(payload.hotkeyAddress, position.chain)
    }

    fun back() = router.back()

    private suspend fun prepare(request: ExitPreparationRequest): BittensorExitDraft? {
        val position = request.position ?: return null
        val value = request.input.text.toBigDecimalOrNull()?.takeIf { it.isPositive } ?: return null
        val amount = value.movePointRight(position.amountPrecision).toBigIntegerExact()
        val quote = interactor.quote(position, amount, request.input.mode == ExitAmountMode.MAX)
        return interactor.prepare(position, quote)
    }

    private fun formatInput(position: BittensorExitPosition, amount: BigInteger): String = tokens.formatToken(
        BigDecimal(amount, position.amountPrecision),
        if (isRoot) position.asset.token.configuration.symbol else TokenSymbol(position.subnet?.symbol?.ifBlank { "—" } ?: "—")
    ).toString()
}
