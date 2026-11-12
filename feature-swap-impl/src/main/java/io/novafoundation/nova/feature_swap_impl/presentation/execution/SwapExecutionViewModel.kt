package io.novafoundation.nova.feature_swap_impl.presentation.execution

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.feature_swap_impl.presentation.execution.model.SwapExecutionSavedState
import io.novafoundation.nova.feature_swap_impl.presentation.execution.model.SwapExecutionSavedState.Status
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_swap_api.domain.model.SwapSubmittedTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapEntryPoint
import io.novafoundation.nova.analytics.AmountBucket
import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.analytics.DurationBucket
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.runtime.binding.DispatchError
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicOperationDisplayData
import io.novafoundation.nova.feature_swap_api.domain.model.SwapOperationSubmissionException
import io.novafoundation.nova.feature_swap_api.domain.model.SwapProgress
import io.novafoundation.nova.feature_swap_api.domain.model.SwapProgressStep
import io.novafoundation.nova.feature_swap_api.domain.model.quotedAmount
import io.novafoundation.nova.feature_swap_api.domain.model.remainingTimeWhenExecuting
import io.novafoundation.nova.feature_swap_api.domain.model.swapDirection
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapSettingsPayload
import io.novafoundation.nova.feature_swap_api.presentation.model.toParcel
import io.novafoundation.nova.feature_swap_api.presentation.view.bottomSheet.description.launchPriceDifferenceDescription
import io.novafoundation.nova.feature_swap_api.presentation.view.bottomSheet.description.launchSlippageDescription
import io.novafoundation.nova.feature_swap_api.presentation.view.bottomSheet.description.launchSwapRateDescription
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.domain.swap.swapRateDescriptionMode
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.common.analytics.toSwapFailureReason
import io.novafoundation.nova.feature_swap_impl.presentation.common.details.SwapConfirmationDetailsFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.fee.createForSwap
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.SwapState
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.SwapOperationLoader
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapOperationPayload
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.feature_swap_impl.presentation.execution.model.SwapProgressModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.model.toAssetPayload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import io.novafoundation.nova.feature_wallet_api.data.repository.UsdRateRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.planksToUsd

class SwapExecutionViewModel(
    private val operationLoader: SwapOperationLoader,
    private val operationPayload: SwapOperationPayload,
    private val savedState: SwapExecutionSavedState,
    private val externalActions: ExternalActions.Presentation,
    private val swapInteractor: SwapInteractor,
    private val resourceManager: ResourceManager,
    private val router: SwapRouter,
    private val chainRegistry: ChainRegistry,
    private val feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    private val confirmationDetailsFormatter: SwapConfirmationDetailsFormatter,
    private val descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher,
    private val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
    private val analyticsService: AnalyticsService,
    private val usdRateRepository: UsdRateRepository,
) : BaseViewModel(),
    Retriable by operationLoader,
    ExternalActions by externalActions,
    DescriptionBottomSheetLauncher by descriptionBottomSheetLauncher,
    ExtrinsicNavigationWrapper by extrinsicNavigationWrapper {

    private val swapStateFlow = operationLoader.state(viewModelScope).shareInBackground()

    private val swapProgressFlow = MutableStateFlow<SwapProgress?>(null)

    private var checkpoint = if (savedState.status == Status.NEW) savedState.copy(status = Status.RUNNING) else savedState

    fun saveExecutionState(): SwapExecutionSavedState = checkpoint

    private val totalSteps = swapStateFlow.map { it.fee.segments.size }

    val swapProgressModel = combine(swapStateFlow, swapProgressFlow) { swapState, swapProgress ->
        when {
            swapProgress != null -> swapProgress.toUi(swapState)
            savedState.status == Status.RUNNING -> SwapProgressModel.Interrupted(
                resourceManager.getString(R.string.swap_execution_interrupted),
                resourceManager.formatDateTime(checkpoint.at),
                stepModels(swapState, checkpoint.stepIndex, unknown = true)
            )
            else -> null
        }
    }.filterNotNull().shareInBackground()

    val feeMixin = feeLoaderMixinFactory.createForSwap(
        chainAssetIn = swapStateFlow.map { it.quote.assetIn },
        interactor = swapInteractor
    )

    val confirmationDetailsFlow = swapStateFlow.map {
        confirmationDetailsFormatter.format(it.quote, it.slippage)
    }.shareInBackground()

    init {
        setFee()

        if (savedState.status == Status.NEW) executeSwap() else restoreProgress()
    }

    private fun restoreProgress() = launchUnit {
        val state = swapStateFlow.first()
        when (savedState.status) {
            Status.COMPLETED -> swapProgressFlow.value = SwapProgress.Done
            Status.FAILED -> {
                val segment = state.fee.segments.getOrNull(savedState.stepIndex) ?: return@launchUnit
                val step = SwapProgressStep(savedState.stepIndex, segment.operation.constructDisplayData(segment.netFlow), segment.operation)
                swapProgressFlow.value = SwapProgress.Failure(IllegalStateException(savedState.reason), step, savedTransaction())
            }
            Status.NEW, Status.RUNNING -> Unit // Never submit again when Android restores an execution screen.
        }
    }

    private fun savedTransaction(): SwapSubmittedTransaction? {
        val hash = checkpoint.transactionHash ?: return null
        val chain = checkpoint.transactionChainId ?: return null
        return SwapSubmittedTransaction(chain, hash, checkpoint.transactionFailed)
    }

    fun transactionClicked() = launchUnit {
        val transaction = savedTransaction() ?: return@launchUnit
        externalActions.showExternalActions(ExternalActions.Type.Extrinsic(transaction.hash), chainRegistry.getChain(transaction.chainId))
    }

    fun rateClicked() = launchUnit {
        val quote = swapStateFlow.first().quote
        launchSwapRateDescription(resourceManager, quote.swapRateDescriptionMode())
    }

    fun priceDifferenceClicked() {
        launchPriceDifferenceDescription(resourceManager)
    }

    fun slippageClicked() {
        launchSlippageDescription(resourceManager)
    }

    fun networkFeeClicked() {
        router.openSwapFee(operationPayload)
    }

    fun routeClicked() {
        router.openSwapRoute(operationPayload)
    }

    fun retryClicked() = launchUnit {
        val swapFailure = swapProgressFlow.first() as? SwapProgress.Failure ?: return@launchUnit
        if (checkpoint.status != Status.FAILED) return@launchUnit
        val failedStep = swapFailure.attemptedStep

        val retrySwapPayload = retrySwapPayload(failedStep)

        router.openRetrySwap(retrySwapPayload)
    }

    fun doneClicked() = launchUnit {
        if (operationPayload.returnToTab) {
            router.back()
        } else {
            router.openBalanceDetails(operationPayload.quote.assetOut)
        }
    }

    private fun retrySwapPayload(failedStep: SwapProgressStep): SwapSettingsPayload.RepeatOperation {
        val failedOperation = failedStep.operation

        return SwapSettingsPayload.RepeatOperation(
            assetIn = failedOperation.assetIn.toAssetPayload(),
            assetOut = failedOperation.assetOut.toAssetPayload(),
            amount = failedOperation.estimatedSwapLimit.quotedAmount,
            direction = failedOperation.estimatedSwapLimit.swapDirection.toParcel(),
            source = SwapEntryPoint.RETRY,
            metaAccountId = operationPayload.metaAccountId,
            slippage = operationPayload.slippage,
        )
    }

    private fun setFee() = launchUnit {
        feeMixin.setFee(swapStateFlow.first().fee)
    }

    private fun executeSwap() = launchUnit {
        val fee = swapStateFlow.first().fee
        val executionStartedAt = System.currentTimeMillis()

        swapInteractor.executeSwap(fee)
            .onEach { progress ->
                updateCheckpoint(progress)
                trackSwapProgress(progress, executionStartedAt)
                swapProgressFlow.emit(progress)
            }
            .inBackground()
            .collect()
    }

    private suspend fun trackSwapProgress(progress: SwapProgress, executionStartedAt: Long) {
        when (progress) {
            is SwapProgress.Done -> trackSwapCompleted(executionStartedAt)
            is SwapProgress.Failure -> analyticsService.track(AnalyticsEvent.SwapFailed(progress.error.toSwapFailureReason()))
            is SwapProgress.StepStarted, is SwapProgress.TransactionTracked -> {}
        }
    }

    private suspend fun trackSwapCompleted(executionStartedAt: Long) {
        val quote = swapStateFlow.first().quote

        // fiat estimation is honestly unavailable without a token rate - skip the event in that case
        val usdAmount = usdRateRepository.planksToUsd(quote.assetIn, quote.planksIn)

        analyticsService.track(
            AnalyticsEvent.SwapCompleted(
                amountBucket = AmountBucket.fromOrUnknown(usdAmount),
                durationBucket = DurationBucket.from(System.currentTimeMillis() - executionStartedAt),
                assetIn = quote.assetIn.symbol.value,
                assetOut = quote.assetOut.symbol.value,
                networkIn = chainRegistry.getChain(quote.assetIn.chainId).name,
                networkOut = chainRegistry.getChain(quote.assetOut.chainId).name
            )
        )
    }

    private suspend fun SwapProgress.toUi(swapState: SwapState): SwapProgressModel {
        return when (this) {
            is SwapProgress.Done -> createCompletedStatus()
            is SwapProgress.Failure -> toUi(swapState)
            is SwapProgress.StepStarted -> toUi(swapState)
            is SwapProgress.TransactionTracked -> SwapProgress.StepStarted(step).toUi(swapState)
        }
    }

    private suspend fun SwapProgress.StepStarted.toUi(swapState: SwapState): SwapProgressModel.InProgress {
        val stepDescription = step.displayData.createInProgressLabel()
        val remainingExecutionTime = swapState.quote.executionEstimate.remainingTimeWhenExecuting(step.index)

        return SwapProgressModel.InProgress(
            stepDescription = stepDescription,
            remainingTime = remainingExecutionTime,
            operationsLabel = swapState.inProgressLabelForStep(step.index)
        )
    }

    private fun SwapState.inProgressLabelForStep(stepIndex: Int): String {
        val totalSteps = fee.segments.size
        val currentStepNumber = stepIndex + 1
        return resourceManager.getString(R.string.swap_execution_operations_progress, currentStepNumber, totalSteps)
    }

    private suspend fun createCompletedStatus(): SwapProgressModel.Completed {
        val totalSteps = totalSteps.first()
        val stepsLabel = resourceManager.getQuantityString(R.plurals.swap_execution_operations_completed, totalSteps, totalSteps)

        return SwapProgressModel.Completed(at = createAtLabel(), operationsLabel = stepsLabel)
    }

    private fun createAtLabel(): String {
        return resourceManager.formatDateTime(checkpoint.at)
    }

    private suspend fun SwapProgress.Failure.toUi(state: SwapState): SwapProgressModel {
        val unknown = !hasKnownOutcome() && savedState.status != Status.FAILED
        val steps = stepModels(state, attemptedStep.index, unknown)
        return if (unknown) {
            SwapProgressModel.Interrupted(resourceManager.getString(R.string.swap_execution_interrupted), createAtLabel(), steps)
        } else {
            SwapProgressModel.Failed(checkpoint.reason ?: createSwapFailureMessage(), createAtLabel(), steps, canRetry = true)
        }
    }

    private suspend fun stepModels(state: SwapState, failedIndex: Int, unknown: Boolean): List<SwapProgressModel.Step> {
        return state.fee.segments.mapIndexed { index, segment ->
            SwapProgressModel.Step(
                label = segment.operation.constructDisplayData(segment.netFlow).createErrorLabel(),
                status = when {
                    index < failedIndex -> SwapProgressModel.StepStatus.COMPLETED
                    index > failedIndex -> SwapProgressModel.StepStatus.PENDING
                    unknown -> SwapProgressModel.StepStatus.UNKNOWN
                    else -> SwapProgressModel.StepStatus.FAILED
                },
                hasTransaction = index == failedIndex && checkpoint.transactionHash != null
            )
        }
    }

    private suspend fun updateCheckpoint(progress: SwapProgress) {
        checkpoint = when (progress) {
            is SwapProgress.StepStarted -> SwapExecutionSavedState(Status.RUNNING, progress.step.index)
            is SwapProgress.TransactionTracked -> checkpoint.copy(
                transactionChainId = progress.transaction.chainId,
                transactionHash = progress.transaction.hash,
                transactionFailed = progress.transaction.failedOnChain
            )
            is SwapProgress.Done -> checkpoint.copy(status = Status.COMPLETED, at = System.currentTimeMillis())
            is SwapProgress.Failure -> checkpoint.copy(
                status = if (progress.hasKnownOutcome()) Status.FAILED else Status.RUNNING,
                at = System.currentTimeMillis(),
                reason = progress.createSwapFailureMessage()
            )
        }
    }

    private suspend fun SwapProgress.Failure.createSwapFailureMessage(): String {
        val failedStepNumber = attemptedStep.index + 1
        val label = attemptedStep.displayData.createErrorLabel()

        val genericErrorMessage = resourceManager.getString(
            R.string.swap_execution_failure,
            failedStepNumber.format(),
            label
        )

        val errorFormatted = formatThrowable()

        return if (errorFormatted != null) {
            "$genericErrorMessage: $errorFormatted"
        } else {
            genericErrorMessage
        }
    }

    private fun SwapProgress.Failure.formatThrowable(): String? {
        return when (val error = error) {
            is SwapOperationSubmissionException.SimulationFailed -> resourceManager.getString(R.string.swap_dry_run_failed_inline_message)

            // Surface on-chain dispatch errors as Pallet.ErrorName (e.g. XYK.MaxInRatioExceeded)
            // instead of dropping the reason entirely
            is DispatchError.Module -> error.toString()

            else -> null
        }
    }

    private suspend fun AtomicOperationDisplayData.createErrorLabel(): String {
        return when (this) {
            is AtomicOperationDisplayData.Swap -> {
                val fromAsset = chainRegistry.asset(from.chainAssetId)
                val toAsset = chainRegistry.asset(to.chainAssetId)
                val on = chainRegistry.getChain(fromAsset.chainId)

                resourceManager.getString(
                    R.string.swap_execution_failure_swap_label,
                    fromAsset.symbol.value,
                    toAsset.symbol.value,
                    on.name
                )
            }

            is AtomicOperationDisplayData.Transfer -> {
                val (chainFrom, assetFrom) = chainRegistry.chainWithAsset(from)
                val chainTo = chainRegistry.getChain(to.chainId)

                resourceManager.getString(
                    R.string.swap_execution_failure_transfer_label,
                    assetFrom.symbol.value,
                    chainFrom.name,
                    chainTo.name,
                )
            }
        }
    }

    private suspend fun AtomicOperationDisplayData.createInProgressLabel(): String {
        return when (this) {
            is AtomicOperationDisplayData.Swap -> {
                val fromAsset = chainRegistry.asset(from.chainAssetId)
                val toAsset = chainRegistry.asset(to.chainAssetId)
                val on = chainRegistry.getChain(fromAsset.chainId)

                resourceManager.getString(
                    R.string.swap_execution_progress_swap_label,
                    fromAsset.symbol.value,
                    toAsset.symbol.value,
                    on.name
                )
            }

            is AtomicOperationDisplayData.Transfer -> {
                val (_, assetFrom) = chainRegistry.chainWithAsset(from)
                val chainTo = chainRegistry.getChain(to.chainId)

                resourceManager.getString(
                    R.string.swap_execution_progress_transfer_label,
                    assetFrom.symbol.value,
                    chainTo.name
                )
            }
        }
    }
}
