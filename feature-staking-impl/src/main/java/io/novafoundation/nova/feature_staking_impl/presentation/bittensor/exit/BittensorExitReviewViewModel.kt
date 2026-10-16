package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.flow.catch
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimateContext
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorEstimatesRepository
import io.novafoundation.nova.common.domain.map
import android.os.SystemClock
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.presentation.fallbackIcon
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.images.asUrlIcon
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicSubmissionRecovery
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicRecoveryState
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.toUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorFeeUi
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.swap.SwapAssetView
import io.novafoundation.nova.feature_wallet_api.presentation.view.swap.SwapAssetsView
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.expectedBlockTime
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private sealed interface ExitExecution {
    data object Idle : ExitExecution
    data class Running(val startedAt: Long) : ExitExecution
    data class Success(val hash: String, val at: Long) : ExitExecution
    data class Failure(val model: BittensorStakeResultUi.Failure, val hash: String? = null) : ExitExecution
    data class Recovered(val model: BittensorStakeResultUi.Recovered) : ExitExecution
}

class BittensorExitReviewViewModel(
    private val payload: BittensorExitReviewPayload,
    private val interactor: BittensorExitInteractor,
    private val router: StakingRouter,
    private val resources: ResourceManager,
    private val icons: AssetIconProvider,
    private val addresses: AddressIconGenerator,
    private val tokens: TokenFormatter,
    private val fiatFormatter: FiatFormatter,
    private val recovery: ExtrinsicSubmissionRecovery,
    chains: ChainRegistry,
    walletUi: WalletUiUseCase,
    accounts: SelectedAccountUseCase,
    chainState: ChainStateRepository,
    private val externalActions: ExternalActions.Presentation,
    estimates: BittensorEstimatesRepository,
    private val logger: Logger
) : BaseViewModel(), ExternalActions by externalActions {

    val isRoot = payload.position.netuid().isRoot()
    val isFullExit = payload.fullExit

    private val chain by lazyAsync { chains.getChain(ChainGeneses.BITTENSOR) }

    val wallet = walletUi.selectedWalletUiFlow().shareInBackground()

    val account = accounts.selectedAddressModelFlow(chain::await).shareInBackground()

    private val confirmMutex = Mutex()

    private val reload = MutableStateFlow(0L)

    private val execution = MutableStateFlow<ExitExecution>(ExitExecution.Idle)

    private val sheetDismissed = MutableStateFlow(false)

    private val prepared = reload.flatMapLatest {
        flowOf {
            val position = interactor.load(payload.position.metaId, payload.position.netuid(), payload.position.hotkeyAddress)
            interactor.prepare(position, payload.quote())
        }.withSafeLoading()
    }.shareInBackground()

    private val estimatedDuration = flowOf { chainState.expectedBlockTime(ChainGeneses.BITTENSOR) }
        .catch { cause ->
            logger.log("[BittensorExit] Execution duration unavailable")
            logger.log(cause)
        }
        .shareInBackground()

    private var recoveringHash: String? = null

    private val pending = recovery.observe(payload.position.metaId, ChainGeneses.BITTENSOR)
        .onEach { state ->
            if (execution.value !is ExitExecution.Running) {
                when (state) {
                    is ExtrinsicRecoveryState.Pending -> {
                        recoveringHash = state.hash
                        execution.value = ExitExecution.Failure(
                            BittensorStakeResultUi.Failure(
                                resources.getString(R.string.bittensor_submission_unknown_title),
                                resources.getString(R.string.bittensor_submission_unknown_description),
                                state.hash,
                                canRetry = false
                            )
                        )
                    }
                    is ExtrinsicRecoveryState.Resolved -> {
                        val shown = (execution.value as? ExitExecution.Failure)?.model?.error
                        if (recoveringHash == state.result.hash || shown == state.result.hash) {
                            execution.value = ExitExecution.Recovered(state.result.toUi(resources, payload.position.metaId))
                            recoveringHash = null
                            sheetDismissed.value = false
                        }
                    }
                    ExtrinsicRecoveryState.None -> Unit
                }
            }
        }.shareInBackground()

    val position = prepared.map { it.dataOrNull?.position }.shareInBackground()

    val error = prepared.map { (it as? ExtendedLoadingState.Error)?.exception?.message }.shareInBackground()

    val title = resources.getString(if (isRoot) R.string.bittensor_exit_root_title else R.string.bittensor_exit_review_sale)

    val primaryAmount = prepared.map {
        it.dataOrNull?.let { draft ->
            amountModel(draft.position, draft.quote.amount, !isRoot).let {
                if (isFullExit) {
                    it.copy(
                        token = "≈ " + it.token
                    )
                } else {
                    it
                }
            }
        }
    }.shareInBackground()

    val assets = prepared.map {
        it.dataOrNull?.let { draft ->
            val inIcon = draft.position.subnet?.logoUrl?.asUrlIcon() ?: AssetIconProvider.fallbackIcon
            val outIcon = icons.getAssetIconOrFallback(draft.position.asset.token.configuration)
            val output = amountModel(draft.position, draft.quote.estimatedTaoOut - draft.quote.serviceFee, false)
            SwapAssetsView.Model(
                SwapAssetView.Model(inIcon, amountModel(draft.position, draft.quote.amount, true), iconSizeDp = 48, clipIconToOutline = true),
                SwapAssetView.Model(outIcon, output.copy(token = "≈ " + output.token), iconSizeDp = 48)
            )
        }
    }.shareInBackground()

    val stakeAfter = prepared.map {
        it.dataOrNull?.let { draft ->
            val before = tokens.formatToken(
                draft.position.asset.token.amountFromPlanks(draft.position.preflight.stake),
                draft.position.asset.token.configuration.symbol
            )
            val after = if (draft.quote.fullExit) {
                BigInteger.ZERO
            } else {
                draft.position.preflight.stake - draft.quote.amount - if (draft.feeFromStake) {
                    draft.fee.maxAmountDeductionFor(draft.position.asset.token.configuration)
                } else {
                    BigInteger.ZERO
                }
            }
            "$before → " + tokens.formatToken(draft.position.asset.token.amountFromPlanks(after), draft.position.asset.token.configuration.symbol)
        } ?: "—"
    }.shareInBackground()

    val rate = prepared.map {
        it.dataOrNull?.let { draft ->
            val ratio = draft.quote.estimatedTaoOut.toBigDecimal().divide(draft.quote.amount.toBigDecimal(), 9, java.math.RoundingMode.DOWN)
            "1 ${draft.position.subnet?.symbol?.ifBlank { "—" } ?: "—"} ≈ " + tokens.formatToken(ratio, draft.position.asset.token.configuration.symbol)
        } ?: "—"
    }.shareInBackground()

    private val costBasisState = estimates.observeCostBasis(
        position.map { it?.takeUnless { it.isRoot }?.let { BittensorEstimateContext(it.metaId, it.chain) } },
        reload
    ).shareInBackground()

    val averagePrice = combine(costBasisState, position) { state, position ->
        state.map { basis ->
            val average = basis?.forSubnet(payload.position.netuid)?.averagePrice
            if (average == null || position == null) "—" else "≈ " + tokens.formatToken(average, position.asset.token.configuration.symbol)
        }
    }.shareInBackground()

    val earned = combine(costBasisState, prepared) { state, prepared ->
        state.map { basis ->
            val draft = prepared.dataOrNull
            val totals = basis?.forSubnet(payload.position.netuid)
            if (draft == null || totals == null) {
                "—"
            } else {
                val profit = totals.estimatedProfit(draft.quote.amount, draft.quote.estimatedTaoOut - draft.quote.serviceFee)
                "≈ " + tokens.formatToken(draft.position.asset.token.amountFromPlanks(profit), draft.position.asset.token.configuration.symbol)
            }
        }
    }.shareInBackground()

    val validator = payload.position.hotkeyAddress

    val validatorIcon = position.map { position ->
        position?.let { addresses.createAddressIcon(it.hotkey.value, 18, AddressIconGenerator.BACKGROUND_TRANSPARENT) }
    }.shareInBackground()

    val fee = prepared.map {
        it.dataOrNull?.let { draft ->
            val formatted = amountModel(draft.position, draft.fee.maxAmountDeductionFor(draft.position.asset.token.configuration), false)
            FeeStatus.Loaded(FeeModel(Unit, FeeDisplay(formatted.token, formatted.fiat)))
        } ?: FeeStatus.Loading(true)
    }.shareInBackground()

    val commissionCaption = prepared.map { state ->
        state.dataOrNull?.let { draft ->
            val fee = tokens.formatToken(
                draft.position.asset.token.amountFromPlanks(draft.quote.serviceFee),
                draft.position.asset.token.configuration.symbol
            )
            resources.getString(R.string.bittensor_exit_service_fee, fee)
        }
    }.shareInBackground()

    val feeCaption = prepared.map { state ->
        val draft = state.dataOrNull ?: return@map null
        val feeSource = resources.getString(
            if (draft.feeFromStake) R.string.bittensor_exit_fee_from_stake else R.string.bittensor_exit_fee_from_free,
            draft.position.asset.token.configuration.symbol.value
        )
        if (isFullExit) resources.getString(R.string.bittensor_exit_full_position) + "\n" + feeSource else feeSource
    }.shareInBackground()

    val isExecuting = execution.map { it is ExitExecution.Running }.shareInBackground()

    val isCompleted = execution.map { it is ExitExecution.Success }.shareInBackground()

    val showExecution = execution.map { !isRoot && (it is ExitExecution.Running || it is ExitExecution.Success) }.shareInBackground()

    val executionSubtitle = combine(position, execution) { position, state ->
        if (state is ExitExecution.Success) {
            resources.formatDateTime(state.at)
        } else {
            resources.getString(
                R.string.bittensor_exit_selling_progress,
                position?.subnet?.name?.ifBlank { resources.getString(R.string.bittensor_subnet_number, payload.position.netuid) }
                    ?: resources.getString(R.string.bittensor_subnet_number, payload.position.netuid),
                position?.asset?.token?.configuration?.symbol?.value.orEmpty()
            )
        }
    }.shareInBackground()

    fun remainingExecutionTime(): Duration? {
        val duration = estimatedDuration.replayCache.lastOrNull() ?: return null
        val started = (execution.value as? ExitExecution.Running)?.startedAt ?: return null
        return (duration - (SystemClock.elapsedRealtime() - started).milliseconds).coerceAtLeast(Duration.ZERO)
    }

    val confirmButtonState = combine(prepared, pending, execution) { prepared, pending, state ->
        val label = resources.getString(R.string.common_confirm)
        val blocked = state is ExitExecution.Running || state is ExitExecution.Success || state is ExitExecution.Recovered && state.model.successful ||
            state is ExitExecution.Failure && !state.model.canRetry
        if (prepared.dataOrNull == null || blocked || pending is ExtrinsicRecoveryState.Pending) {
            DescriptiveButtonState.Disabled(label)
        } else {
            DescriptiveButtonState.Enabled(label)
        }
    }.shareInBackground()

    val resultState = combine(prepared, execution, sheetDismissed) { prepared, state, dismissed ->
        if (dismissed) return@combine null
        when (state) {
            is ExitExecution.Failure -> state.model
            is ExitExecution.Recovered -> state.model
            is ExitExecution.Running -> if (isRoot) {
                BittensorStakeResultUi.Progress(
                    resources.getString(R.string.bittensor_exit_unstaking_progress),
                    resources.getString(R.string.common_do_not_close_app)
                )
            } else {
                null
            }
            is ExitExecution.Success -> if (isRoot) {
                prepared.dataOrNull?.let { draft ->
                    BittensorStakeResultUi.Success(
                        resources.getString(R.string.bittensor_exit_root_completed),
                        resources.getString(R.string.bittensor_exit_root_completed_description),
                        validator,
                        BittensorFeeUi("≈ " + tokens.formatToken(draft.fee.decimalAmount, draft.fee.asset.symbol), "—")
                    )
                }
            } else {
                null
            }
            ExitExecution.Idle -> null
        }
    }.shareInBackground()

    fun retry() = reload.update { it + 1 }

    fun confirmClicked() = launchUnit {
        if (!confirmMutex.tryLock()) return@launchUnit
        try {
            if (confirmButtonState.first() !is DescriptiveButtonState.Enabled) return@launchUnit
            val draft = prepared.first().dataOrNull ?: return@launchUnit
            execution.value = ExitExecution.Running(SystemClock.elapsedRealtime())
            sheetDismissed.value = false
            try {
                when (val result = interactor.execute(draft)) {
                    is BittensorOperationResult.Success -> execution.value = ExitExecution.Success(result.transactionHash, System.currentTimeMillis())
                    is BittensorOperationResult.Failure -> execution.value = ExitExecution.Failure(
                        BittensorStakeResultUi.Failure(
                            resources.getString(R.string.common_failed),
                            resources.getString(R.string.bittensor_staking_failed_description),
                            result.cause.message ?: resources.getString(R.string.common_retry_message)
                        ),
                        result.transactionHash
                    )
                    is BittensorOperationResult.Unknown -> execution.value = ExitExecution.Failure(
                        BittensorStakeResultUi.Failure(
                            resources.getString(R.string.bittensor_submission_unknown_title),
                            resources.getString(R.string.bittensor_submission_unknown_description),
                            result.transactionHash,
                            canRetry = false
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                execution.value = ExitExecution.Failure(
                    BittensorStakeResultUi.Failure(
                        resources.getString(R.string.common_failed),
                        resources.getString(R.string.common_retry_message),
                        e.message.orEmpty()
                    )
                )
            }
        } finally {
            confirmMutex.unlock()
        }
    }

    fun accountClicked() = launchUnit { externalActions.showAddressActions(account.first().address, chain.await()) }

    fun validatorClicked() = launchUnit { externalActions.showAddressActions(validator, chain.await()) }

    fun resultDismissed() {
        sheetDismissed.value = true
        launchUnit { acknowledgeResult() }
    }

    fun doneClicked() = launchUnit {
        acknowledgeResult()
        router.openBittensorPortfolio()
    }

    fun back() { if (execution.value !is ExitExecution.Running) router.back() }

    private suspend fun acknowledgeResult() {
        val hash = when (val state = execution.value) {
            is ExitExecution.Success -> state.hash
            is ExitExecution.Recovered -> state.model.transactionHash
            is ExitExecution.Failure -> state.hash
            else -> null
        }
        hash?.let { recovery.acknowledge(payload.position.metaId, it) }
    }

    private fun amountModel(
        position: io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitPosition,
        planks: BigInteger,
        alpha: Boolean
    ): AmountModel {
        val value = if (alpha) BigDecimal(planks, position.amountPrecision) else position.asset.token.amountFromPlanks(planks)
        val tao = if (alpha) position.subnet?.priceInRao?.let { value * position.asset.token.amountFromPlanks(it) } else value
        val fiat = tao?.let(position.asset.token::toFiatOrNull)
        val symbol = if (alpha) TokenSymbol(position.subnet?.symbol?.ifBlank { "—" } ?: "—") else position.asset.token.configuration.symbol
        return AmountModel(
            tokens.formatToken(value, symbol),
            fiat?.let { fiatFormatter.formatFiat(it, position.asset.token.currency) }
        )
    }
}
