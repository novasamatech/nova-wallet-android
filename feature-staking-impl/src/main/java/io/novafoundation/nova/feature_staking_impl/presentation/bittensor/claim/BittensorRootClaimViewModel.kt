package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.claim

import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.StakingFlow
import io.novafoundation.nova.analytics.StakingValidationStage
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.analyticsKind
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorResultNavigation
import io.novafoundation.nova.analytics.BittensorDataEndpoint
import io.novafoundation.nova.analytics.BittensorScreen
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorReadAnalytics
import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorOperationAnalytics

import io.novafoundation.nova.runtime.extrinsic.DryRunRpcException

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isSigningCancelled

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.createSubstrateAddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.duration.DurationFormatter
import io.novasama.substrate_sdk_android.hash.isPositive
import kotlin.time.Duration.Companion.milliseconds
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.BuildConfig
import io.novafoundation.nova.common.data.storage.DebugSettings
import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicDryRunResult
import kotlinx.coroutines.TimeoutCancellationException
import io.novafoundation.nova.common.presentation.ellipsizeAddress
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationOutcome
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootValidatorDetails
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorClaimValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorClaimValidationPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorClaimValidationFailureUi
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.formatBittensorNativeResult
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.formatNetworkFee
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
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorResultAction
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.BittensorValidatorInfoPayload
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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    private val analytics: AnalyticsService,
    private val operationAnalytics: BittensorOperationAnalytics,
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
    private val durationFormatter: DurationFormatter,
    private val amountFormatter: BittensorAmountFormatter,
    private val debugSettings: DebugSettings
) : BaseViewModel(), ExternalActions by externalActions, Validatable by validationExecutor {

    private val confirmMutex = Mutex()

    private val resultNavigation = BittensorResultNavigation(
        router,
        analytics,
        StakingFlow.CLAIM,
        StakingAnalyticsTarget.ROOT,
        newPosition = false,
        trackActions = !(BuildConfig.DEBUG && payload.previewOnly)
    )

    private var isClaimInitiatedTracked = false

    private val previewOnly = BuildConfig.DEBUG && payload.previewOnly

    private val dryRunRunning = MutableStateFlow(false)

    val dryRunResult = MutableStateFlow<String?>(null)

    private val validating = MutableStateFlow(false)

    private val claimPreviewsReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.CLAIM_PREVIEWS, BittensorScreen.CLAIM)

    val retryEvents = retryController.events

    private val execution = MutableStateFlow<ClaimExecution>(ClaimExecution.Idle)

    private val dismissed = MutableStateFlow(false)

    private val previewRequest = MutableStateFlow(ClaimPreviewRequest())

    private val state = previewRequest.flatMapLatest { request ->
        retryController.observe(
            step = "root claim preview",
            analytics = claimPreviewsReads,
            transform = { it.withSafeLoading() }
        ) {
            flowOf {
                bittensorRootClaimInteractor.load(payload.metaId, request.hotkey).copy(
                    previewOnly = previewOnly,
                    allowBelowMinimumPreview = debugSettings.bittensorClaimButtonEnabled()
                )
            }
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
            previewOnly -> resources.getString(R.string.bittensor_claim_preview_notice)
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

    val amount = state.mapLoading { claim ->
        claim.selectedPreview?.let { preview ->
            val token = claim.asset.token
            val value = token.amountFromPlanks(preview.redeemable)
            val formatted = amountFormatter.amount(
                value,
                token.configuration.symbol,
                token.toFiatOrNull(value),
                token.currency
            )
            formatted.copy(token = "≈ ${formatted.token}")
        } ?: AmountModel("—", null)
    }
        .shareInBackground()

    private val validatorDetails = claimState.mapNotNull { it?.chain }
        .distinctUntilChanged()
        .flatMapLatest { chain ->
            flowOf { bittensorRootClaimInteractor.validatorDetails(chain) }.withSafeLoading()
        }
        .shareInBackground()

    val validator = combine(state, validatorDetails) { state, details ->
        state.map { claim ->
            claim.selectedPreview?.let { preview -> validatorName(claim, preview.hotkey, details.dataOrNull) } ?: "—"
        }
    }
        .shareInBackground()

    // Root validators of the account: the claim result names the validator when it is the only one, as on iOS.
    private val rootHotkeys = claimState.filterNotNull()
        .distinctUntilChangedBy { it.coldkey }
        .flatMapLatest(bittensorRootClaimInteractor::observeRootHotkeys)
        .shareInBackground()

    val stakeAfterClaim = state.mapLoading { claim ->
        val stake = claim.claims.rootStake
        val preview = claim.selectedPreview
        if (stake == null || preview == null) {
            null
        } else {
            val token = claim.asset.token
            val before = tokenFormatter.formatToken(token.amountFromPlanks(stake), token.configuration.symbol)
            val after = tokenFormatter.formatToken(token.amountFromPlanks(claim.stakeAfterClaim(preview.redeemable)), token.configuration.symbol)
            "$before → ≈ $after"
        }
    }.shareInBackground()

    val disclosure = combine(state, validator) { loaded, validator ->
        val claim = loaded.dataOrNull ?: return@combine null
        val preview = claim.selectedPreview ?: return@combine null
        val name = validator.dataOrNull ?: claim.chain.addressOf(preview.hotkey.value).ellipsizeAddress()
        buildList {
            add(resources.getString(R.string.bittensor_claim_restake_notice, name))
            claim.claims.rootHoldDurationMillis?.takeIf { it.isPositive() }?.let { duration ->
                add(
                    resources.getString(
                        R.string.bittensor_claim_hold_notice,
                        name,
                        durationFormatter.format(duration.toLong().milliseconds)
                    )
                )
            }
            if (preview.forfeitedEstimate.isPositive()) {
                val forfeiture = amountFormatter.minimumAmount(
                    claim.asset.token.amountFromPlanks(preview.forfeitedEstimate),
                    claim.asset.token.configuration.symbol
                )
                add(resources.getString(R.string.bittensor_claim_forfeit_notice, forfeiture))
            }
            if (claim.claims.claimable.any { it.hotkey != preview.hotkey }) {
                add(resources.getString(R.string.bittensor_claim_other_validators_notice))
            }
        }.joinToString("\n")
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

    val confirmButtonState = combine(
        prepared,
        execution,
        validating,
        dryRunRunning,
        debugSettings.observeBittensorClaim()
    ) { prepared, execution, validating, dryRunRunning, debugEnabled ->
        val label = resources.getString(if (previewOnly) R.string.bittensor_claim_dry_run else R.string.bittensor_claim_action)
        val result = (execution as? ClaimExecution.Result)
            ?.model
        val blocked = execution is ClaimExecution.Running || result is BittensorStakeResultUi.Success || result is BittensorStakeResultUi.Unconfirmed ||
            result is BittensorStakeResultUi.Failure && !result.canRetry
        if (validating || dryRunRunning) {
            DescriptiveButtonState.Loading
        } else if (previewOnly != debugEnabled || prepared.dataOrNull == null || blocked) {
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
        prepared.onEach { loading ->
            val draft = loading.dataOrNull
            if (!isClaimInitiatedTracked && !previewOnly && draft != null && draft.state.supportsImmediateOperations &&
                draft.state.availableClaims.any { it.hotkey == draft.preview.hotkey }
            ) {
                isClaimInitiatedTracked = true
                analytics.track(
                    BittensorAnalyticsEvent.ClaimInitiated(
                        BittensorAnalyticsEvent.NETWORK,
                        null,
                        draft.state.availableClaims.size
                    )
                )
            }
        }.launchIn(this)
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

    val isDryRun: Boolean get() = previewOnly

    fun confirmClicked() {
        if (previewOnly) {
            runDryRun()
        } else {
            confirmWithProfit(null)
        }
    }

    private fun runDryRun() = launchUnit {
        if (!previewOnly || !debugSettings.bittensorClaimEnabled() || !confirmMutex.tryLock()) return@launchUnit
        try {
            if (confirmButtonState.first() !is DescriptiveButtonState.Enabled) return@launchUnit
            val current = claimState.first() ?: return@launchUnit
            dryRunRunning.value = true
            dryRunResult.value = null
            val result = bittensorRootClaimInteractor.dryRun(current)
            dryRunResult.value = when (result) {
                ExtrinsicDryRunResult.Success ->
                    resources.getString(R.string.bittensor_claim_dry_run_success)
                is ExtrinsicDryRunResult.DispatchFailure ->
                    resources.getString(R.string.bittensor_claim_dry_run_dispatch_failure, result.description)
                is ExtrinsicDryRunResult.InvalidTransaction ->
                    resources.getString(R.string.bittensor_claim_dry_run_invalid, result.description)
            }
        } catch (e: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            dryRunResult.value = resources.getString(R.string.bittensor_claim_dry_run_error, "timeout")
        } catch (e: CancellationException) {
            throw e
        } catch (e: SigningCancelledException) {
            dryRunResult.value = resources.getString(R.string.bittensor_claim_dry_run_cancelled)
        } catch (e: DryRunRpcException) {
            dryRunResult.value = resources.getString(R.string.bittensor_claim_dry_run_error, e.diagnostic)
        } catch (e: UnsupportedOperationException) {
            dryRunResult.value = resources.getString(R.string.bittensor_claim_dry_run_error, e.message.orEmpty())
        } catch (e: Exception) {
            // Do not log signed extrinsics or RPC payloads.
            dryRunResult.value = resources.getString(R.string.bittensor_claim_dry_run_error, e.javaClass.simpleName)
        } finally {
            dryRunRunning.value = false
            confirmMutex.unlock()
        }
    }

    fun dryRunResultShown() {
        dryRunResult.value = null
    }

    private fun confirmWithProfit(accepted: BittensorClaimValidationPayload?): Unit = launchUnit {
        if (previewOnly || debugSettings.bittensorClaimEnabled()) return@launchUnit
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
                        onOutcome = { outcome ->
                            failure.reason.analyticsKind()?.let { kind ->
                                analytics.track(
                                    BittensorAnalyticsEvent.ValidationShown(
                                        StakingFlow.CLAIM,
                                        kind,
                                        StakingValidationStage.CONFIRM,
                                        outcome,
                                        StakingAnalyticsTarget.ROOT
                                    )
                                )
                            }
                        },
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
            execution.value = claimResult(e.mapBittensorPreSubmissionError().mapResult(resources))
        } finally {
            validating.value = false
            confirmMutex.unlock()
        }
    }

    private suspend fun submit(draft: BittensorRootClaimDraft, validated: BittensorClaimValidationPayload) {
        if (previewOnly || debugSettings.bittensorClaimEnabled()) {
            execution.value = ClaimExecution.Idle
            return
        }
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
            operationAnalytics.startOperation()
            val result = bittensorRootClaimInteractor.execute(draft, lease)
            operationAnalytics.outcome(result, StakingAnalyticsTarget.ROOT, StakingFlow.CLAIM)
            execution.value = when (result) {
                is BittensorOperationResult.Success -> ClaimExecution.Result(mapClaimed(draft, result.outcome))
                is BittensorOperationResult.Failure -> if (result.isSigningCancelled) {
                    ClaimExecution.Idle
                } else {
                    claimResult(result.mapResult(resources))
                }
                is BittensorOperationResult.Unknown -> claimResult(result.mapResult(resources))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.log("[BittensorClaim] Claim failed")
            logger.log(e)
            val result = e.mapBittensorSubmissionError()
            operationAnalytics.outcome(result, StakingAnalyticsTarget.ROOT, StakingFlow.CLAIM)
            execution.value = claimResult(result.mapResult(resources))
        }
    }

    /** iOS `createClaimDoneSheet`: a claim that redeemed nothing only charged the fee and can only be closed. */
    private suspend fun mapClaimed(draft: BittensorRootClaimDraft, outcome: BittensorOperationOutcome?): BittensorStakeResultUi {
        val claim = draft.state
        val fee = outcome.formatNetworkFee(draft.fee, claim.chain, resources, tokenFormatter)
        val claimed = outcome?.tao
        if (claimed != null && claimed.isZero) {
            return BittensorStakeResultUi.Failure(
                title = resources.getString(R.string.bittensor_claim_nothing_claimed),
                description = resources.getString(R.string.bittensor_claim_fee_charged),
                error = resources.getString(R.string.bittensor_claim_below_minimum_reason),
                canRetry = false,
                fee = fee
            )
        }

        val amount = formatBittensorNativeResult(claimed, draft.preview.redeemable, claim.chain, tokenFormatter).token
        val stakeAfter = formatBittensorNativeResult(
            claimed?.let(claim::stakeAfterClaim),
            claim.stakeAfterClaim(draft.preview.redeemable),
            claim.chain,
            tokenFormatter
        ).token.toString()
        val groupHotkeys = rootHotkeys.replayCache.lastOrNull().orEmpty() + draft.preview.hotkey
        val description = if (groupHotkeys.size == 1) {
            val name = validatorName(claim, draft.preview.hotkey, validatorDetails.first().dataOrNull)
            resources.getString(R.string.bittensor_claim_root_stake_now_with, stakeAfter, name)
        } else {
            resources.getString(R.string.bittensor_claim_root_stake_now, stakeAfter)
        }

        return BittensorStakeResultUi.Success(
            title = resources.getString(R.string.bittensor_claim_success_title, amount),
            description = description,
            validator = null,
            fee = fee,
            outcome = outcome,
            stakeAfter = stakeAfter,
            actions = resultNavigation.landingActions()
        )
    }

    private fun claimResult(model: BittensorStakeResultUi): ClaimExecution.Result {
        val landed = if (model is BittensorStakeResultUi.Unconfirmed) model.copy(actions = resultNavigation.landingActions()) else model
        return ClaimExecution.Result(landed)
    }

    private fun validatorName(claim: BittensorRootClaimState, hotkey: AccountIdKey, details: Map<AccountIdKey, BittensorRootValidatorDetails>?): String =
        details?.get(hotkey)?.validator?.name?.takeIf { it.isNotBlank() } ?: claim.chain.addressOf(hotkey.value).ellipsizeAddress()

    /** A result sheet button, as iOS `activate(action:)`; Try again hides the sheet so the claim can be confirmed again. */
    fun resultActionClicked(action: BittensorResultAction) {
        if (action !in resultModel()?.actions.orEmpty()) return

        resultNavigation.activate(action) { dismissed.value = true }
    }

    private fun resultModel() = (execution.value as? ClaimExecution.Result)?.model

    private fun isFinished() = resultModel().let { it is BittensorStakeResultUi.Success || it is BittensorStakeResultUi.Unconfirmed }

    fun validatorClicked() = launchUnit {
        prepared.first().dataOrNull?.let {
            router.openBittensorValidatorInfo(
                BittensorValidatorInfoPayload(BittensorNetuid.Root.value, it.state.chain.addressOf(it.preview.hotkey.value))
            )
        }
    }

    fun back() {
        when {
            execution.value is ClaimExecution.Running || dryRunRunning.value -> Unit
            isFinished() -> resultNavigation.leave()
            else -> router.back()
        }
    }
}
