package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer.Payload
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer.Payload.DialogAction
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.hasTheSaveValueAs
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.SimpleFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.GenericDecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.networkFeeByRequestedAccount
import io.novafoundation.nova.feature_wallet_api.presentation.model.networkFeeByRequestedAccountOrZero
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.math.BigDecimal

private val FEE_RATIO_THRESHOLD = 1.5.toBigDecimal()

class FeeChangeValidation<P, E, F : GenericFee>(
    private val calculateFee: suspend (P) -> GenericDecimalFee<F>,
    private val currentFee: GenericFeeProducer<F, P>,
    private val chainAsset: (P) -> Chain.Asset,
    private val error: (FeeChangeDetectedFailure.Payload<F>) -> E
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val oldFee = currentFee(value)
        val newFee = calculateFee(value)

        val oldAmount = oldFee.networkFeeByRequestedAccountOrZero
        val newAmount = newFee.networkFeeByRequestedAccount

        val areFeesSame = oldAmount hasTheSaveValueAs newAmount

        return areFeesSame isTrueOrError {
            val payload = FeeChangeDetectedFailure.Payload(
                needsUserAttention = newAmount / oldAmount > FEE_RATIO_THRESHOLD,
                oldFee = oldAmount,
                newFee = newFee,
                chainAsset = chainAsset(value)
            )

            error(payload)
        }
    }
}

interface FeeChangeDetectedFailure<T : GenericFee> {

    class Payload<T : GenericFee>(
        val needsUserAttention: Boolean,
        val oldFee: BigDecimal,
        val newFee: GenericDecimalFee<T>,
        val chainAsset: Chain.Asset,
    )

    val payload: Payload<T>
}

fun <P, E> ValidationSystemBuilder<P, E>.checkForSimpleFeeChanges(
    calculateFee: suspend (P) -> Fee,
    currentFee: FeeProducer<P>,
    chainAsset: (P) -> Chain.Asset,
    error: (FeeChangeDetectedFailure.Payload<SimpleFee>) -> E
) {
    checkForFeeChanges(
        calculateFee = { payload -> SimpleFee(calculateFee(payload)) },
        currentFee = currentFee,
        chainAsset = chainAsset,
        error = error
    )
}

fun <P, E, F : GenericFee> ValidationSystemBuilder<P, E>.checkForFeeChanges(
    calculateFee: suspend (P) -> F,
    currentFee: GenericFeeProducer<F, P>,
    chainAsset: (P) -> Chain.Asset,
    error: (FeeChangeDetectedFailure.Payload<F>) -> E
) = validate(
    FeeChangeValidation(
        calculateFee = { payload ->
            val newFee = calculateFee(payload)

            GenericDecimalFee(
                genericFee = calculateFee(payload),
                networkFeeDecimalAmount = chainAsset(payload).amountFromPlanks(newFee.networkFee.amount)
            )
        },
        currentFee = currentFee,
        error = error,
        chainAsset = chainAsset
    )
)

fun <T : GenericFee> CoroutineScope.handleFeeSpikeDetected(
    error: FeeChangeDetectedFailure<T>,
    resourceManager: ResourceManager,
    feeLoaderMixin: FeeLoaderMixin.Presentation,
    actions: ValidationFlowActions<*>
): TransformedFailure? = handleFeeSpikeDetected(
    error = error,
    resourceManager = resourceManager,
    actions = actions,
    setFee = { feeLoaderMixin.setFee(it.newFee.networkFee) }
)

fun <T : GenericFee> CoroutineScope.handleFeeSpikeDetected(
    error: FeeChangeDetectedFailure<T>,
    resourceManager: ResourceManager,
    actions: ValidationFlowActions<*>,
    setFee: suspend (error: FeeChangeDetectedFailure.Payload<T>) -> Unit,
): TransformedFailure? {
    if (!error.payload.needsUserAttention) {
        actions.resumeFlow()
        return null
    }

    val chainAsset = error.payload.chainAsset
    val oldFee = error.payload.oldFee.formatTokenAmount(chainAsset)
    val newFee = error.payload.newFee.networkFeeByRequestedAccount.formatTokenAmount(chainAsset)

    return TransformedFailure.Custom(
        Payload(
            title = resourceManager.getString(R.string.common_fee_changed_title),
            message = resourceManager.getString(R.string.common_fee_changed_message, newFee, oldFee),
            customStyle = R.style.AccentNegativeAlertDialogTheme_Reversed,
            okAction = DialogAction(
                title = resourceManager.getString(R.string.common_proceed),
                action = {
                    launch {
                        setFee(error.payload)
                        actions.resumeFlow()
                    }
                }
            ),
            cancelAction = DialogAction(
                title = resourceManager.getString(R.string.common_refresh_fee),
                action = {
                    launch {
                        setFee(error.payload)
                    }
                }
            )
        )
    )
}
