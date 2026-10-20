package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.claim

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isSigningCancelled

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.createSubstrateAddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.duration.DurationFormatter
import io.novasama.substrate_sdk_android.hash.isPositive
import kotlin.time.Duration.Companion.milliseconds
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorClaimValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorClaimValidationPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorClaimValidationFailureUi
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.formatNetworkFee
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.formatBittensorNativeResult
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mapBittensorPreSubmissionError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mapBittensorSubmissionError
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.mapResult
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.message
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootClaimDraft
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootClaimInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootClaimState
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFeeEstimate
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.forRequest
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.asFeeContextFromSelf
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefaultBy
import io.novafoundation.nova.runtime.ext.addressOf
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.sync.Mutex

private sealed interface ClaimExecution {
    data object Idle : ClaimExecution
    data object Running : ClaimExecution
    data class Result(val model: BittensorStakeResultUi) : ClaimExecution
}

private data class ClaimPreviewRequest(val hotkey: AccountIdKey? = null, val revision: Long = 0)

class BittensorRootClaimViewModel(
    private val payload: BittensorRootClaimPayload,
    private val bittensorRootClaimInteractor: BittensorRootClaimInteractor,
    private val router: StakingRouter,
    private val resources: ResourceManager,
    private val tokenFormatter: TokenFormatter,
    private val addressIconGenerator: AddressIconGenerator,
    private val externalActions: ExternalActions.Presentation,
    private val logger: Logger,
    private val walletUiUseCase: WalletUiUseCase,
    private val retryController: BittensorRetryController,
    private val validationExecutor: ValidationExecutor,
    private val claimValidationSystem: BittensorClaimValidationSystem,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    private val durationFormatter: DurationFormatter
) : BaseViewModel(), ExternalActions by externalActions, Validatable by validationExecutor {

    private val confirmMutex = Mutex()

    private val validating = MutableStateFlow(false)

    val retryEvents = retryController.events

    private val execution = MutableStateFlow<ClaimExecution>(ClaimExecution.Idle)

    private val dismissed = MutableStateFlow(false)

    private val previewRequest = MutableStateFlow(ClaimPreviewRequest())

    private val state = previewRequest.flatMapLatest { request ->
        retryController.observe(
            step = "root claim preview",
            transform = { it.withSafeLoading() }
        ) {
            flowOf { bittensorRootClaimInteractor.load(payload.metaId, request.hotkey) }
        }
    }
        .shareInBackground()

    private val claimState = state.map { it.dataOrNull }
        .shareInBackground()

    private val feeContext = claimState.mapNotNull { it?.asset?.token?.configuration }
        .asFeeContextFromSelf()

    val feeLoaderMixin = feeLoaderMixinFactory.createDefaultBy<BittensorFeeEstimate<BittensorRootClaimState>>(
        scope = this,
        feeContext = feeContext
    )

    val feeStatus = combine(
        claimState,
        feeLoaderMixin.fee
    ) { state, status -> status.forRequest(state) }
        .shareInBackground()

    private val prepared = combine(
        claimState,
        feeStatus
    ) { state, status ->
        val fee = status.loadedFeeOrNull()
        val draft = if (state == null || fee == null || fee.request != state) null else bittensorRootClaimInteractor.prepare(state, fee)
        ExtendedLoadingState.Loaded(draft)
    }
        .shareInBackground()

    val warning = state.map { state ->
        when {
            state.dataOrNull?.let { claim ->
                claim.selectedHotkey?.let { it in claim.pendingHotkeys } == true || claim.availableClaims.isEmpty() && claim.pendingHotkeys.isNotEmpty()
            } == true -> resources.getString(R.string.bittensor_claim_pending)
            state.dataOrNull != null && state.dataOrNull?.selectedPreview == null -> resources.getString(R.string.bittensor_claim_none)
            else -> null
        }
    }
        .shareInBackground()

    val wallet = state.flatMapLatest { state ->
        state.dataOrNull?.let { walletUiUseCase.walletUiFlow(it.metaId) } ?: emptyFlow()
    }
        .shareInBackground()

    val account = state.map { it.dataOrNull }
        .flatMapLatest { state ->
            if (state == null) {
                emptyFlow()
            } else {
                flowOf {
                    addressIconGenerator.createSubstrateAddressModel(state.chain.addressOf(state.coldkey.value), 24)
                }
            }
        }
        .shareInBackground()

    val feeCaption = state.map { state ->
        val symbol = state.dataOrNull?.asset?.token?.configuration?.symbol?.value ?: return@map null
        resources.getString(R.string.bittensor_exit_fee_from_free, symbol)
    }
        .shareInBackground()

    val amount = state.mapLoading { claim ->
        claim.selectedPreview?.let { preview ->
            tokenFormatter.formatToken(
                claim.asset.token.amountFromPlanks(preview.redeemable),
                claim.asset.token.configuration.symbol
            )
        } ?: "—"
    }
        .shareInBackground()

    val forfeited = state.mapLoading { claim ->
        claim.selectedPreview?.let { preview ->
            tokenFormatter.formatToken(
                claim.asset.token.amountFromPlanks(preview.forfeitedEstimate),
                claim.asset.token.configuration.symbol
            )
        } ?: "—"
    }
        .shareInBackground()

    val validator = state.mapLoading { claim ->
        claim.selectedPreview
            ?.let { claim.chain.addressOf(it.hotkey.value) } ?: "—"
    }
        .shareInBackground()

    val stakeAfterClaim = state.mapLoading { claim ->
        val stake = claim.claims.rootStake
        val preview = claim.selectedPreview
        if (stake == null || preview == null) {
            null
        } else {
            val before = tokenFormatter.formatToken(claim.asset.token.amountFromPlanks(stake), claim.asset.token.configuration.symbol)
            val after = tokenFormatter.formatToken(claim.asset.token.amountFromPlanks(stake + preview.redeemable), claim.asset.token.configuration.symbol)
            "$before → $after"
        }
    }.shareInBackground()

    val disclosure = state.map { loaded ->
        val claim = loaded.dataOrNull ?: return@map null
        val preview = claim.selectedPreview ?: return@map null
        buildList {
            add(resources.getString(R.string.bittensor_claim_restake_notice, claim.chain.addressOf(preview.hotkey.value)))
            claim.claims.rootHoldDurationMillis?.takeIf { it.isPositive() }?.let { duration ->
                add(resources.getString(R.string.bittensor_claim_hold_notice, durationFormatter.format(duration.toLong().milliseconds)))
            }
            if (preview.forfeitedEstimate.isPositive()) {
                val forfeiture = tokenFormatter.formatToken(
                    claim.asset.token.amountFromPlanks(preview.forfeitedEstimate),
                    claim.asset.token.configuration.symbol
                )
                add(resources.getString(R.string.bittensor_claim_forfeit_notice, forfeiture))
            }
        }.joinToString("\n\n")
    }.shareInBackground()

    val validatorIcon = prepared.map {
        it.dataOrNull?.let { draft ->
            addressIconGenerator.createAddressIcon(
                draft.preview.hotkey.value,
                18,
                AddressIconGenerator.BACKGROUND_TRANSPARENT
            )
        }
    }
        .shareInBackground()

    val fee = prepared.map { it.dataOrNull?.let { draft -> tokenFormatter.formatToken(draft.fee.decimalAmount, draft.fee.asset.symbol) } ?: "—" }
        .shareInBackground()

    val remaining = state.map {
        resources.getString(
            R.string.bittensor_claim_other_hotkeys,
            ((it.dataOrNull?.claims?.claimable?.size ?: 0) - 1)
                .coerceAtLeast(0)
        )
    }
        .shareInBackground()

    val confirmButtonState = combine(
        prepared,
        execution,
        validating
    ) { prepared, execution, validating ->
        val label = resources.getString(R.string.bittensor_claim_action)
        val result = (execution as? ClaimExecution.Result)
            ?.model
        val blocked = execution is ClaimExecution.Running || result is BittensorStakeResultUi.Success || result is BittensorStakeResultUi.Unconfirmed ||
            result is BittensorStakeResultUi.Failure && !result.canRetry
        if (validating) {
            DescriptiveButtonState.Loading
        } else if (prepared.dataOrNull == null || blocked) {
            DescriptiveButtonState.Disabled(label)
        } else {
            DescriptiveButtonState.Enabled(label)
        }
    }
        .shareInBackground()

    val resultState = combine(
        execution,
        dismissed
    ) { state, dismissed ->
        if (dismissed) {
            null
        } else {
            when (state) {
                ClaimExecution.Idle -> null
                ClaimExecution.Running -> BittensorStakeResultUi.Progress(
                    resources.getString(R.string.bittensor_claim_progress),
                    resources.getString(R.string.common_do_not_close_app)
                )
                is ClaimExecution.Result -> state.model
            }
        }
    }
        .shareInBackground()

    init {
        feeLoaderMixin.connectWith(claimState) { _, state ->
            state?.let { claimState ->
                bittensorRootClaimInteractor.estimateFee(claimState)
                    ?.let { BittensorFeeEstimate(claimState, it) }
            }
        }
    }

    fun accountClicked() = launchUnit {
        state.first().dataOrNull?.let { externalActions.showAddressActions(it.chain.addressOf(it.coldkey.value), it.chain) }
    }

    fun confirmClicked(): Unit = confirmWithProfit(null)

    private fun confirmWithProfit(accepted: BittensorClaimValidationPayload?): Unit = launchUnit {
        if (!confirmMutex.tryLock()) return@launchUnit
        try {
            if (confirmButtonState.first() !is DescriptiveButtonState.Enabled) return@launchUnit
            val draft = prepared.first().dataOrNull ?: return@launchUnit
            validating.value = true
            val request = previewRequest.value
            val freshPayload = bittensorRootClaimInteractor.validationPayload(draft)
            val validationPayload = freshPayload.copy(
                uneconomicClaimAccepted = accepted != null && accepted.current == freshPayload.current && accepted.fee == freshPayload.fee
            )
            validationExecutor.requireValid(
                validationSystem = claimValidationSystem,
                payload = validationPayload,
                validationFailureTransformerCustom = { failure, _ ->
                    bittensorClaimValidationFailureUi(
                        failure.reason,
                        resources,
                        reviewUpdated = {
                            if (previewRequest.value == request) {
                                previewRequest.value = ClaimPreviewRequest(draft.preview.hotkey, request.revision + 1)
                            }
                        },
                        acceptUneconomic = { if (previewRequest.value == request) confirmWithProfit(validationPayload) }
                    )
                }
            ) {
                if (previewRequest.value == request) {
                    execution.value = ClaimExecution.Running
                    launchUnit { submit(draft, validationPayload) }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            dismissed.value = false
            execution.value = ClaimExecution.Result(e.mapBittensorPreSubmissionError().mapResult(resources))
        } finally {
            validating.value = false
            confirmMutex.unlock()
        }
    }

    private suspend fun submit(draft: BittensorRootClaimDraft, validated: BittensorClaimValidationPayload) {
        val lease = bittensorRootClaimInteractor.tryAcquire(draft)
        if (lease == null) {
            execution.value = ClaimExecution.Idle
            validationExecutor.requireValid(
                validationSystem = claimValidationSystem,
                payload = validated.copy(claimPending = true),
                validationFailureTransformer = { failure ->
                    resources.getString(R.string.common_error_general_title) to failure.message(resources)
                }
            ) { }
            return
        }
        execution.value = ClaimExecution.Running
        dismissed.value = false
        try {
            execution.value = when (val result = bittensorRootClaimInteractor.execute(draft, lease)) {
                is BittensorOperationResult.Success -> ClaimExecution.Result(
                    BittensorStakeResultUi.Success(
                        resources.getString(R.string.bittensor_claim_completed),
                        resources.getString(R.string.bittensor_claim_completed_description),
                        draft.state.chain.addressOf(draft.preview.hotkey.value),
                        result.outcome.formatNetworkFee(draft.fee, draft.state.chain, resources, tokenFormatter),
                        amount = formatBittensorNativeResult(result.outcome?.tao, draft.preview.redeemable, draft.state.chain, tokenFormatter)
                    )
                )
                is BittensorOperationResult.Failure -> if (result.isSigningCancelled) {
                    ClaimExecution.Idle
                } else {
                    ClaimExecution.Result(result.mapResult(resources))
                }
                is BittensorOperationResult.Unknown -> ClaimExecution.Result(result.mapResult(resources))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.log("[BittensorClaim] Claim failed")
            logger.log(e)
            execution.value = ClaimExecution.Result(e.mapBittensorSubmissionError().mapResult(resources))
        }
    }

    fun resultDismissed() {
        dismissed.value = true
    }

    fun done() {
        resultDismissed()
        router.openBittensorPortfolio()
    }

    fun validatorClicked() = launchUnit {
        prepared.first().dataOrNull?.let {
            externalActions.showAddressActions(
                it.state.chain.addressOf(it.preview.hotkey.value),
                it.state.chain
            )
        }
    }

    fun back() {
        if (execution.value !is ClaimExecution.Running) router.back()
    }
}
