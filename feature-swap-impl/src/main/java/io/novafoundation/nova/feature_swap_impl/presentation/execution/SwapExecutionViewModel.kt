package io.novafoundation.nova.feature_swap_impl.presentation.execution

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
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
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SwapFlowScopeAggregator
import io.novafoundation.nova.feature_swap_api.presentation.view.bottomSheet.description.launchPriceDifferenceDescription
import io.novafoundation.nova.feature_swap_api.presentation.view.bottomSheet.description.launchSlippageDescription
import io.novafoundation.nova.feature_swap_api.presentation.view.bottomSheet.description.launchSwapRateDescription
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.domain.interactor.SwapInteractor
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_swap_impl.presentation.common.details.SwapConfirmationDetailsFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.fee.createForSwap
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.SwapState
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.SwapStateStoreProvider
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.getStateOrThrow
import io.novafoundation.nova.feature_swap_impl.presentation.execution.model.SwapProgressModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.model.toAssetPayload
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class SwapExecutionViewModel(
    private val swapStateStoreProvider: SwapStateStoreProvider,
    private val swapInteractor: SwapInteractor,
    private val resourceManager: ResourceManager,
    private val router: SwapRouter,
    private val chainRegistry: ChainRegistry,
    private val feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    private val confirmationDetailsFormatter: SwapConfirmationDetailsFormatter,
    private val descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher,
    private val swapFlowScopeAggregator: SwapFlowScopeAggregator,
    private val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
) : BaseViewModel(),
    DescriptionBottomSheetLauncher by descriptionBottomSheetLauncher,
    ExtrinsicNavigationWrapper by extrinsicNavigationWrapper {

    private val swapFlowScope = swapFlowScopeAggregator.getFlowScope(viewModelScope)

    private val swapStateFlow = flowOf { swapStateStoreProvider.getStateOrThrow(swapFlowScope) }

    private val swapProgressFlow = singleReplaySharedFlow<SwapProgress>()

    private val totalSteps = swapStateFlow.map { it.fee.segments.size }

    val swapProgressModel = combine(swapStateFlow, swapProgressFlow) { swapState, swapProgress ->
        swapProgress.toUi(swapState)
    }.shareInBackground()

    val feeMixin = feeLoaderMixinFactory.createForSwap(
        chainAssetIn = swapStateFlow.map { it.quote.assetIn },
        interactor = swapInteractor
    )

    val confirmationDetailsFlow = swapStateFlow.map {
        confirmationDetailsFormatter.format(it.quote, it.slippage)
    }.shareInBackground()

    init {
        setFee()

        executeSwap()
    }

    fun rateClicked() {
        launchSwapRateDescription()
    }

    fun priceDifferenceClicked() {
        launchPriceDifferenceDescription()
    }

    fun slippageClicked() {
        launchSlippageDescription()
    }

    fun networkFeeClicked() {
        router.openSwapFee()
    }

    fun routeClicked() {
        router.openSwapRoute()
    }

    fun retryClicked() = launchUnit {
        val swapFailure = swapProgressFlow.first() as? SwapProgress.Failure ?: return@launchUnit
        val failedStep = swapFailure.attemptedStep

        val retrySwapPayload = retrySwapPayload(failedStep)

        router.openRetrySwap(retrySwapPayload)
    }

    fun doneClicked() = launchUnit {
        val assetOut = swapStateFlow.first().quote.assetOut.fullId.toAssetPayload()
        router.openBalanceDetails(assetOut)
    }

    private fun retrySwapPayload(failedStep: SwapProgressStep): SwapSettingsPayload {
        val failedOperation = failedStep.operation

        return SwapSettingsPayload.RepeatOperation(
            assetIn = failedOperation.assetIn.toAssetPayload(),
            assetOut = failedOperation.assetOut.toAssetPayload(),
            amount = failedOperation.estimatedSwapLimit.quotedAmount,
            direction = failedOperation.estimatedSwapLimit.swapDirection.toParcel(),
        )
    }

    private fun setFee() = launchUnit {
        feeMixin.setFee(swapStateFlow.first().fee)
    }

    private fun executeSwap() = launchUnit {
        val fee = swapStateFlow.first().fee

        swapInteractor.executeSwap(fee)
            .onEach(swapProgressFlow::emit)
            .inBackground()
            .collect()
    }

    private suspend fun SwapProgress.toUi(swapState: SwapState): SwapProgressModel {
        return when (this) {
            is SwapProgress.Done -> createCompletedStatus()
            is SwapProgress.Failure -> toUi()
            is SwapProgress.StepStarted -> toUi(swapState)
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
        val currentTime = System.currentTimeMillis()
        return resourceManager.formatDateTime(currentTime)
    }

    private suspend fun SwapProgress.Failure.toUi(): SwapProgressModel.Failed {
        return SwapProgressModel.Failed(
            reason = createSwapFailureMessage(),
            at = createAtLabel()
        )
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
            "${genericErrorMessage}: $errorFormatted"
        } else {
            genericErrorMessage
        }
    }

    private fun SwapProgress.Failure.formatThrowable(): String? {
        if (error !is SwapOperationSubmissionException) return null

        // For some reason smart-cast does not work here
        return when (error as SwapOperationSubmissionException) {
            is SwapOperationSubmissionException.SimulationFailed -> resourceManager.getString(R.string.swap_dry_run_failed_inline_message)
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
