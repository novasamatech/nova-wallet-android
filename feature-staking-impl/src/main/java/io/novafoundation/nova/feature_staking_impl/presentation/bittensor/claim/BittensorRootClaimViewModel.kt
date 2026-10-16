package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.claim

import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.common.address.createSubstrateAddressModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootClaimInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicSubmissionRecovery
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicRecoveryState
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.toUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorFeeUi
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.runtime.ext.addressOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger

private sealed interface ClaimExecution {
    data object Idle : ClaimExecution
    data object Running : ClaimExecution
    data class Result(val metaId: Long, val hash: String?, val model: BittensorStakeResultUi) : ClaimExecution
}

class BittensorRootClaimViewModel(
    private val interactor: BittensorRootClaimInteractor,
    private val router: StakingRouter,
    private val resources: ResourceManager,
    private val tokens: TokenFormatter,
    private val addresses: AddressIconGenerator,
    private val recovery: ExtrinsicSubmissionRecovery,
    private val externalActions: ExternalActions.Presentation,
    private val logger: Logger,
    private val walletUi: WalletUiUseCase
) : BaseViewModel(), ExternalActions by externalActions {
    private val confirmMutex = Mutex()

    private val reload = MutableStateFlow(0L)

    private val execution = MutableStateFlow<ClaimExecution>(ClaimExecution.Idle)

    private val dismissed = MutableStateFlow(false)

    private val state = combine(interactor.observeWalletId(), reload) { walletId, _ -> walletId }
        .flatMapLatest { flowOf { interactor.load() }.withSafeLoading() }
        .shareInBackground()

    private val prepared = state.map { it.dataOrNull }
        .flatMapLatest { state ->
            flowOf {
                state ?: return@flowOf null
                val target = state.claims.claimable.firstOrNull()?.hotkey ?: return@flowOf null
                interactor.prepare(state, target)
            }.withSafeLoading()
        }.shareInBackground()

    private val pending = state.flatMapLatest { state ->
        state.dataOrNull?.let { recovery.observe(it.metaId, it.chain.id) } ?: kotlinx.coroutines.flow.flowOf(ExtrinsicRecoveryState.None)
    }.onEach { pending ->
        val current = execution.value as? ClaimExecution.Result
        if (pending is ExtrinsicRecoveryState.Resolved && current?.hash == pending.result.hash &&
            current.model is BittensorStakeResultUi.Failure && !current.model.canRetry
        ) {
            execution.value = current.copy(model = pending.result.toUi(resources, current.metaId))
        }
    }.shareInBackground()

    val error = combine(state, prepared, pending) { state, prepared, pending ->
        val cause = (state as? ExtendedLoadingState.Error)?.exception ?: (prepared as? ExtendedLoadingState.Error)?.exception
        when {
            pending is ExtrinsicRecoveryState.Pending -> resources.getString(R.string.bittensor_submission_unknown_description)
            cause != null -> {
                logger.log("[BittensorClaim] Claim preview unavailable")
                logger.log(cause)
                cause.message ?: resources.getString(
                    R.string.common_retry_message
                )
            }
            state.dataOrNull?.claims?.claimable?.isEmpty() == true -> resources.getString(R.string.bittensor_claim_none)
            else -> null
        }
    }.shareInBackground()

    val wallet = state.flatMapLatest { state ->
        state.dataOrNull?.let { walletUi.walletUiFlow(it.metaId) } ?: kotlinx.coroutines.flow.emptyFlow()
    }.shareInBackground()

    val account = state.map { it.dataOrNull }.flatMapLatest { state ->
        if (state == null) {
            kotlinx.coroutines.flow.emptyFlow()
        } else {
            flowOf {
                addresses.createSubstrateAddressModel(state.chain.addressOf(state.coldkey.value), 24)
            }
        }
    }.shareInBackground()

    val feeStatus = prepared.map {
        it.dataOrNull?.let { draft ->
            FeeStatus.Loaded(FeeModel(Unit, FeeDisplay(tokens.formatToken(draft.fee.decimalAmount, draft.fee.asset.symbol), null)))
        } ?: FeeStatus.Loading(true)
    }.shareInBackground()

    fun accountClicked() = launchUnit { state.first().dataOrNull?.let { externalActions.showAddressActions(it.chain.addressOf(it.coldkey.value), it.chain) } }

    val feeCaption = state.map { state ->
        val symbol = state.dataOrNull?.asset?.token?.configuration?.symbol?.value ?: return@map null
        resources.getString(R.string.bittensor_exit_fee_from_free, symbol)
    }.shareInBackground()

    val amount = prepared.map {
        it.dataOrNull?.let { draft ->
            tokens.formatToken(
                draft.state.asset.token.amountFromPlanks(draft.preview.redeemable),
                draft.state.asset.token.configuration.symbol
            )
        } ?: "—"
    }.shareInBackground()

    val forfeited = prepared.map {
        it.dataOrNull?.let { draft ->
            tokens.formatToken(
                draft.state.asset.token.amountFromPlanks(draft.preview.forfeitedEstimate),
                draft.state.asset.token.configuration.symbol
            )
        } ?: "—"
    }.shareInBackground()

    val validator = prepared.map { it.dataOrNull?.let { draft -> draft.state.chain.addressOf(draft.preview.hotkey.value) } ?: "—" }.shareInBackground()

    val validatorIcon = prepared.map {
        it.dataOrNull?.let { draft ->
            addresses.createAddressIcon(
                draft.preview.hotkey.value,
                18,
                AddressIconGenerator.BACKGROUND_TRANSPARENT
            )
        }
    }.shareInBackground()

    val fee = prepared.map { it.dataOrNull?.let { draft -> tokens.formatToken(draft.fee.decimalAmount, draft.fee.asset.symbol) } ?: "—" }.shareInBackground()

    val remaining = state.map {
        resources.getString(
            R.string.bittensor_claim_other_hotkeys,
            ((it.dataOrNull?.claims?.claimable?.size ?: 0) - 1).coerceAtLeast(0)
        )
    }.shareInBackground()

    val confirmButtonState = combine(prepared, pending, execution) { prepared, pending, execution ->
        val label = resources.getString(R.string.bittensor_claim_action)
        val result = (execution as? ClaimExecution.Result)?.model
        val blocked = execution is ClaimExecution.Running || result is BittensorStakeResultUi.Success ||
            result is BittensorStakeResultUi.Recovered && result.successful || result is BittensorStakeResultUi.Failure && !result.canRetry
        if (prepared.dataOrNull == null || blocked || pending is ExtrinsicRecoveryState.Pending) {
            DescriptiveButtonState.Disabled(label)
        } else {
            DescriptiveButtonState.Enabled(label)
        }
    }.shareInBackground()

    val resultState = combine(execution, dismissed) { state, dismissed ->
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
    }.shareInBackground()

    init { pending.launchIn(this) }

    fun confirmClicked() = launchUnit {
        if (!confirmMutex.tryLock()) return@launchUnit
        try {
            if (confirmButtonState.first() !is DescriptiveButtonState.Enabled) return@launchUnit
            val draft = prepared.first().dataOrNull ?: return@launchUnit
            execution.value = ClaimExecution.Running
            dismissed.value = false
            try {
                execution.value = when (val result = interactor.execute(draft)) {
                    is BittensorOperationResult.Success -> ClaimExecution.Result(
                        draft.state.metaId,
                        result.transactionHash,
                        BittensorStakeResultUi.Success(
                            resources.getString(R.string.bittensor_claim_completed),
                            resources.getString(R.string.bittensor_claim_completed_description),
                            draft.state.chain.addressOf(draft.preview.hotkey.value),
                            BittensorFeeUi("≈ " + tokens.formatToken(draft.fee.decimalAmount, draft.fee.asset.symbol), "—")
                        )
                    )
                    is BittensorOperationResult.Failure -> ClaimExecution.Result(draft.state.metaId, result.transactionHash, failure(result.cause.message))
                    is BittensorOperationResult.Unknown -> ClaimExecution.Result(
                        draft.state.metaId,
                        result.transactionHash,
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
                execution.value = ClaimExecution.Result(draft.state.metaId, null, failure(e.message))
            }
        } finally {
            confirmMutex.unlock()
        }
    }

    private fun failure(message: String?) = BittensorStakeResultUi.Failure(
        resources.getString(R.string.common_failed),
        resources.getString(R.string.common_retry_message),
        message ?: resources.getString(R.string.common_retry_message)
    )

    fun resultDismissed() = launchUnit { acknowledgeResult() }

    private suspend fun acknowledgeResult() {
        dismissed.value = true
        val result = execution.value as? ClaimExecution.Result ?: return
        result.hash?.let { recovery.acknowledge(result.metaId, it) }
    }

    fun done() = launchUnit {
        acknowledgeResult()
        router.openBittensorPortfolio()
    }

    fun retry() { reload.value++ }

    fun validatorClicked() = launchUnit {
        prepared.first().dataOrNull?.let {
            externalActions.showAddressActions(
                it.state.chain.addressOf(it.preview.hotkey.value),
                it.state.chain
            )
        }
    }

    fun back() { if (execution.value !is ClaimExecution.Running) router.back() }
}
