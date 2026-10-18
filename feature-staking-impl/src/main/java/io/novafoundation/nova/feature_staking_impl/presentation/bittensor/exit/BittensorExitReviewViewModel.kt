package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import android.os.SystemClock
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.presentation.fallbackIcon
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.common.utils.images.asUrlIcon
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorEstimatesMixin
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimateContext
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitDraft
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFeeEstimate
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.forRequest
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.message
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorFeeUi
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.asFeeContextFromSelf
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefaultBy
import io.novafoundation.nova.feature_wallet_api.presentation.view.swap.SwapAssetView
import io.novafoundation.nova.feature_wallet_api.presentation.view.swap.SwapAssetsView
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.expectedBlockTime
import java.math.BigInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.sync.Mutex

private sealed interface ExitExecution {
    data object Idle : ExitExecution
    data class Running(val startedAt: Long, val estimatedDuration: Duration) : ExitExecution
    data class Success(val at: Long) : ExitExecution
    data class Failure(val model: BittensorStakeResultUi.Failure) : ExitExecution
}

class BittensorExitReviewViewModel(
    private val payload: BittensorExitReviewPayload,
    private val bittensorExitInteractor: BittensorExitInteractor,
    private val router: StakingRouter,
    private val resources: ResourceManager,
    private val assetIconProvider: AssetIconProvider,
    private val addressIconGenerator: AddressIconGenerator,
    private val tokenFormatter: TokenFormatter,
    private val fiatFormatter: FiatFormatter,
    chainRegistry: ChainRegistry,
    walletUiUseCase: WalletUiUseCase,
    accountRepository: AccountRepository,
    private val chainStateRepository: ChainStateRepository,
    private val externalActions: ExternalActions.Presentation,
    estimatesMixin: BittensorEstimatesMixin,
    private val retryController: BittensorRetryController,
    private val validationExecutor: ValidationExecutor,
    private val exitValidationSystem: BittensorExitValidationSystem,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    private val exitFormatter: BittensorExitFormatter
) : BaseViewModel(), ExternalActions by externalActions, Validatable by validationExecutor {

    val isRoot = payload.position.netuid().isRoot()

    val isFullExit = payload.fullExit

    val slippage = bittensorExitInteractor.slippage.fraction.movePointRight(2).toStripTrailingZerosString() + "%"

    private val chain by lazyAsync { chainRegistry.getChain(ChainGeneses.BITTENSOR) }

    val wallet = walletUiUseCase.walletUiFlow(payload.position.metaId)
        .shareInBackground()

    val account = accountRepository.metaAccountFlow(payload.position.metaId)
        .map { walletUiUseCase.walletAddressModel(it, chain.await(), AddressIconGenerator.SIZE_MEDIUM) }
        .shareInBackground()

    private val confirmMutex = Mutex()

    val retryEvents = retryController.events

    private val validating = MutableStateFlow(false)

    private val execution = MutableStateFlow<ExitExecution>(ExitExecution.Idle)

    private val sheetDismissed = MutableStateFlow(false)

    private val requestState = retryController.observe(
        step = "exit confirmation",
        transform = { it.withSafeLoading() }
    ) {
        flowOf {
            val position = bittensorExitInteractor.load(payload.position.metaId, payload.position.netuid(), payload.position.hotkeyAddress)
            BittensorExitRequest(position, payload.quote())
        }
    }
        .shareInBackground()

    private val request = requestState.map { it.dataOrNull }
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
        val request = state.dataOrNull
        val estimate = status.loadedFeeOrNull()
        when {
            request == null -> state.map { null }
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

    init {
        feeLoaderMixin.connectWith(request) { _, request ->
            request?.let {
                val fee = bittensorExitInteractor.estimateFee(it.position, it.quote)
                BittensorFeeEstimate(it, fee)
            }
        }
    }

    val position = prepared.map { it.dataOrNull?.position }
        .shareInBackground()

    val title = resources.getString(if (isRoot) R.string.bittensor_exit_root_title else R.string.bittensor_exit_review_sale)

    val primaryAmount = prepared.map {
        it.dataOrNull?.let { draft ->
            exitFormatter.amount(draft.position, draft.quote.amount, !isRoot).let {
                if (isFullExit) {
                    it.copy(
                        token = "≈ " + it.token
                    )
                } else {
                    it
                }
            }
        }
    }
        .shareInBackground()

    val assets = prepared.map {
        it.dataOrNull?.let { draft ->
            val inIcon = draft.position.subnet?.logoUrl?.asUrlIcon() ?: AssetIconProvider.fallbackIcon
            val outIcon = assetIconProvider.getAssetIconOrFallback(draft.position.asset.token.configuration)
            val output = exitFormatter.amount(draft.position, draft.quote.estimatedTaoOut - draft.quote.serviceFee, false)
            SwapAssetsView.Model(
                SwapAssetView.Model(
                    inIcon,
                    exitFormatter.amount(draft.position, draft.quote.amount, true),
                    iconSizeDp = 48,
                    clipIconToOutline = true
                ),
                SwapAssetView.Model(outIcon, output.copy(token = "≈ " + output.token), iconSizeDp = 48)
            )
        }
    }
        .shareInBackground()

    val stakeAfter = prepared.map {
        it.dataOrNull?.let { draft ->
            val before = tokenFormatter.formatToken(
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
            "$before → " + tokenFormatter.formatToken(draft.position.asset.token.amountFromPlanks(after), draft.position.asset.token.configuration.symbol)
        } ?: "—"
    }
        .shareInBackground()

    val rate = prepared.map { exitFormatter.exchangeRate(it.dataOrNull) }
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
            val average = basis?.forSubnet(payload.position.netuid)
                ?.averagePrice
            if (average == null || position == null) "—" else "≈ " + tokenFormatter.formatToken(average, position.asset.token.configuration.symbol)
        }
    }
        .shareInBackground()

    val earned = combine(
        costBasisState,
        prepared
    ) { state, prepared ->
        state.map { basis ->
            val draft = prepared.dataOrNull
            val totals = basis?.forSubnet(payload.position.netuid)
            if (draft == null || totals == null) {
                "—"
            } else {
                val profit = totals.estimatedProfit(draft.quote.amount, draft.quote.estimatedTaoOut - draft.quote.serviceFee)
                "≈ " + tokenFormatter.formatToken(draft.position.asset.token.amountFromPlanks(profit), draft.position.asset.token.configuration.symbol)
            }
        }
    }
        .shareInBackground()

    val validator = payload.position.hotkeyAddress

    val validatorIcon = position.map { position ->
        position?.let { addressIconGenerator.createAddressIcon(it.hotkey.value, 18, AddressIconGenerator.BACKGROUND_TRANSPARENT) }
    }
        .shareInBackground()

    val fee = feeStatus

    val commissionCaption = prepared.map { state ->
        state.dataOrNull?.let { draft ->
            val fee = tokenFormatter.formatToken(
                draft.position.asset.token.amountFromPlanks(draft.quote.serviceFee),
                draft.position.asset.token.configuration.symbol
            )
            resources.getString(R.string.bittensor_exit_service_fee, fee)
        }
    }
        .shareInBackground()

    val feeCaption = prepared.map { state ->
        val draft = state.dataOrNull ?: return@map null
        val feeSource = resources.getString(
            if (draft.feeFromStake) R.string.bittensor_exit_fee_from_stake else R.string.bittensor_exit_fee_from_free,
            draft.position.asset.token.configuration.symbol.value
        )
        if (isFullExit) resources.getString(R.string.bittensor_exit_full_position) + "\n" + feeSource else feeSource
    }
        .shareInBackground()

    val isExecuting = execution.map { it is ExitExecution.Running }
        .shareInBackground()

    val isCompleted = execution.map { it is ExitExecution.Success }
        .shareInBackground()

    val showExecution = execution.map { !isRoot && (it is ExitExecution.Running || it is ExitExecution.Success) }
        .shareInBackground()

    val executionSubtitle = combine(
        position,
        execution
    ) { position, state ->
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
    }
        .shareInBackground()

    fun remainingExecutionTime(): Duration {
        val running = execution.value as? ExitExecution.Running ?: return Duration.ZERO
        val elapsed = (SystemClock.elapsedRealtime() - running.startedAt).milliseconds
        return (running.estimatedDuration - elapsed).coerceAtLeast(Duration.ZERO)
    }

    val confirmButtonState = combine(
        prepared,
        execution,
        validating
    ) { prepared, state, validating ->
        val label = resources.getString(R.string.common_confirm)
        val blocked = state is ExitExecution.Running || state is ExitExecution.Success ||
            state is ExitExecution.Failure && !state.model.canRetry
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
        prepared,
        execution,
        sheetDismissed
    ) { prepared, state, dismissed ->
        if (dismissed) return@combine null
        when (state) {
            is ExitExecution.Failure -> state.model
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
                        BittensorFeeUi("≈ " + tokenFormatter.formatToken(draft.fee.decimalAmount, draft.fee.asset.symbol), "—")
                    )
                }
            } else {
                null
            }
            ExitExecution.Idle -> null
        }
    }
        .shareInBackground()

    fun confirmClicked() = launchUnit {
        if (!confirmMutex.tryLock()) return@launchUnit
        try {
            if (confirmButtonState.first() !is DescriptiveButtonState.Enabled) return@launchUnit
            val draft = prepared.first().dataOrNull ?: return@launchUnit
            validating.value = true
            val estimatedDuration = chainStateRepository.expectedBlockTime(ChainGeneses.BITTENSOR)
            val validationPayload = bittensorExitInteractor.validationPayload(
                draft.position,
                draft.quote,
                draft.fee,
                draft.feeFromStake
            )
            validationExecutor.requireValid(
                validationSystem = exitValidationSystem,
                payload = validationPayload,
                validationFailureTransformer = { resources.getString(R.string.common_error_general_title) to it.message(resources) }
            ) {
                execution.value = ExitExecution.Running(SystemClock.elapsedRealtime(), estimatedDuration)
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

    private suspend fun submit(draft: BittensorExitDraft) {
        sheetDismissed.value = false
        try {
            when (val result = bittensorExitInteractor.execute(draft)) {
                is BittensorOperationResult.Success -> execution.value = ExitExecution.Success(System.currentTimeMillis())
                is BittensorOperationResult.Failure -> execution.value = ExitExecution.Failure(
                    BittensorStakeResultUi.Failure(
                        resources.getString(R.string.common_failed),
                        resources.getString(R.string.bittensor_staking_failed_description),
                        result.cause.message ?: resources.getString(R.string.common_retry_message)
                    )
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
    }

    fun accountClicked() = launchUnit {
        externalActions.showAddressActions(
            account.first().address,
            chain.await()
        )
    }

    fun validatorClicked() = launchUnit {
        externalActions.showAddressActions(validator, chain.await())
    }

    fun resultDismissed() {
        sheetDismissed.value = true
    }

    fun doneClicked() = launchUnit {
        router.openBittensorPortfolio()
    }

    fun back() {
        if (execution.value !is ExitExecution.Running) router.back()
    }
}
