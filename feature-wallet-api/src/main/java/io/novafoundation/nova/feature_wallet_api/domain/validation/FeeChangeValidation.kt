package io.novafoundation.nova.feature_wallet_api.domain.validation

import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer.Payload
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer.Payload.DialogAction
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.hasTheSaveValueAs
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_account_api.data.model.decimalAmountByExecutingAccount
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFeeLoaderMixin
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.math.BigDecimal

private val FEE_RATIO_THRESHOLD = 1.5.toBigDecimal()

class FeeChangeValidation<P, E, F : SubmissionFee>(
    private val calculateFee: FeeProducer<F, P>,
    private val currentFee: OptionalFeeProducer<F, P>,
    private val chainAsset: (P) -> Chain.Asset,
    private val error: (FeeChangeDetectedFailure.Payload<F>) -> E
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val oldFee = currentFee(value)
        val newFee = calculateFee(value)

        val oldAmount = oldFee?.decimalAmountByExecutingAccount.orZero()
        val newAmount = newFee.decimalAmountByExecutingAccount

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

interface FeeChangeDetectedFailure<F : Fee> {

    class Payload<F : Fee>(
        val needsUserAttention: Boolean,
        val oldFee: BigDecimal,
        val newFee: F,
        val chainAsset: Chain.Asset,
    )

    val payload: Payload<F>
}

fun <P, E, F : Fee> ValidationSystemBuilder<P, E>.checkForFeeChanges(
    calculateFee: suspend (P) -> F,
    currentFee: OptionalFeeProducer<F, P>,
    chainAsset: (P) -> Chain.Asset,
    error: (FeeChangeDetectedFailure.Payload<F>) -> E
) = validate(
    FeeChangeValidation(
        calculateFee = calculateFee,
        currentFee = currentFee,
        error = error,
        chainAsset = chainAsset
    )
)

fun <F : Fee> CoroutineScope.handleFeeSpikeDetected(
    error: FeeChangeDetectedFailure<F>,
    resourceManager: ResourceManager,
    feeLoaderMixin: GenericFeeLoaderMixin.Presentation<F>,
    actions: ValidationFlowActions<*>
): TransformedFailure? = handleFeeSpikeDetected(
    error = error,
    resourceManager = resourceManager,
    actions = actions,
    setFee = { feeLoaderMixin.setFee(it.newFee) }
)

fun <F : Fee> CoroutineScope.handleFeeSpikeDetected(
    error: FeeChangeDetectedFailure<F>,
    resourceManager: ResourceManager,
    actions: ValidationFlowActions<*>,
    setFee: suspend (error: FeeChangeDetectedFailure.Payload<F>) -> Unit,
): TransformedFailure? {
    if (!error.payload.needsUserAttention) {
        actions.resumeFlow()
        return null
    }

    val chainAsset = error.payload.chainAsset
    val oldFee = error.payload.oldFee.formatTokenAmount(chainAsset)
    val newFee = error.payload.newFee.decimalAmountByExecutingAccount.formatTokenAmount(chainAsset)

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
