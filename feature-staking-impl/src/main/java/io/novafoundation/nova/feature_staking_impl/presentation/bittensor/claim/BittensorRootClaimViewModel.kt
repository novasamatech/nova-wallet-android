package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.claim

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.createSubstrateAddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
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
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootClaimDraft
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootClaimInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootClaimState
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFeeEstimate
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.forRequest
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.message
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorFeeUi
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
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory
) : BaseViewModel(), ExternalActions by externalActions, Validatable by validationExecutor {

    private val confirmMutex = Mutex()

    private val validating = MutableStateFlow(false)

    val retryEvents = retryController.events

    private val execution = MutableStateFlow<ClaimExecution>(ClaimExecution.Idle)

    private val dismissed = MutableStateFlow(false)

    private val state = retryController.observe(
        step = "root claim preview",
        transform = { it.withSafeLoading() }
    ) {
        flowOf { bittensorRootClaimInteractor.load(payload.metaId) }
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
            state.dataOrNull?.claims?.claimable?.isEmpty() == true -> resources.getString(R.string.bittensor_claim_none)
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

    fun accountClicked() = launchUnit {
        state.first().dataOrNull?.let { externalActions.showAddressActions(it.chain.addressOf(it.coldkey.value), it.chain) }
    }

    val feeCaption = state.map { state ->
        val symbol = state.dataOrNull?.asset?.token?.configuration?.symbol?.value ?: return@map null
        resources.getString(R.string.bittensor_exit_fee_from_free, symbol)
    }
        .shareInBackground()

    val amount = prepared.map {
        it.dataOrNull?.let { draft ->
            tokenFormatter.formatToken(
                draft.state.asset.token.amountFromPlanks(draft.preview.redeemable),
                draft.state.asset.token.configuration.symbol
            )
        } ?: "—"
    }
        .shareInBackground()

    val forfeited = prepared.map {
        it.dataOrNull?.let { draft ->
            tokenFormatter.formatToken(
                draft.state.asset.token.amountFromPlanks(draft.preview.forfeitedEstimate),
                draft.state.asset.token.configuration.symbol
            )
        } ?: "—"
    }
        .shareInBackground()

    val validator = prepared.map { it.dataOrNull?.let { draft -> draft.state.chain.addressOf(draft.preview.hotkey.value) } ?: "—" }
        .shareInBackground()

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
        val blocked = execution is ClaimExecution.Running || result is BittensorStakeResultUi.Success ||
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

    fun confirmClicked() = launchUnit {
        if (!confirmMutex.tryLock()) return@launchUnit
        try {
            if (confirmButtonState.first() !is DescriptiveButtonState.Enabled) return@launchUnit
            val draft = prepared.first().dataOrNull ?: return@launchUnit
            validating.value = true
            val validationPayload = bittensorRootClaimInteractor.validationPayload(draft)
            validationExecutor.requireValid(
                validationSystem = claimValidationSystem,
                payload = validationPayload,
                validationFailureTransformer = { resources.getString(R.string.common_error_general_title) to it.message(resources) }
            ) {
                execution.value = ClaimExecution.Running
                launchUnit { submit(draft) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showError(e)
        } finally {
            validating.value = false
            confirmMutex.unlock()
        }
    }

    private suspend fun submit(draft: BittensorRootClaimDraft) {
        execution.value = ClaimExecution.Running
        dismissed.value = false
        try {
            execution.value = when (val result = bittensorRootClaimInteractor.execute(draft)) {
                is BittensorOperationResult.Success -> ClaimExecution.Result(
                    BittensorStakeResultUi.Success(
                        resources.getString(R.string.bittensor_claim_completed),
                        resources.getString(R.string.bittensor_claim_completed_description),
                        draft.state.chain.addressOf(draft.preview.hotkey.value),
                        BittensorFeeUi("≈ " + tokenFormatter.formatToken(draft.fee.decimalAmount, draft.fee.asset.symbol), "—")
                    )
                )
                is BittensorOperationResult.Failure -> ClaimExecution.Result(failure(result.cause.message))
                is BittensorOperationResult.Unknown -> ClaimExecution.Result(
                    BittensorStakeResultUi.Failure(
                        resources.getString(R.string.bittensor_submission_unknown_title),
                        resources.getString(R.string.bittensor_submission_unknown_description),
                        result.transactionHash,
                        canRetry = false
                    )
                )
            }
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            logger.log("[BittensorClaim] Claim failed")
            logger.log(e)
            execution.value = ClaimExecution.Result(failure(e.message))
        }
    }

    private fun failure(message: String?) = BittensorStakeResultUi.Failure(
        resources.getString(R.string.common_failed),
        resources.getString(R.string.common_retry_message),
        message ?: resources.getString(R.string.common_retry_message)
    )

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
