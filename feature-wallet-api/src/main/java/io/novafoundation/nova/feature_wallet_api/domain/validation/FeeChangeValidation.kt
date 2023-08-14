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
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.math.BigDecimal

// TODO update testing threshold!!
private val FEE_RATIO_THRESHOLD = 0.toBigDecimal()

class FeeChangeValidation<P, E>(
    private val calculateFee: suspend (P) -> DecimalFee,
    private val currentFee: (P) -> BigDecimal,
    private val chainAsset: (P) -> Chain.Asset,
    private val error: (FeeChangeDetectedFailure.Payload) -> E
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val oldFee = currentFee(value)
        val newFee = calculateFee(value)

        val areFeesSame = oldFee hasTheSaveValueAs newFee.decimalAmount

        return areFeesSame isTrueOrError {
            val payload = FeeChangeDetectedFailure.Payload(
                needsUserAttention = newFee.decimalAmount / oldFee > FEE_RATIO_THRESHOLD,
                oldFee = oldFee,
                newFee = newFee,
                chainAsset = chainAsset(value)
            )

            error(payload)
        }
    }
}

interface FeeChangeDetectedFailure {

    class Payload(
        val needsUserAttention: Boolean,
        val oldFee: BigDecimal,
        val newFee: DecimalFee,
        val chainAsset: Chain.Asset,
    )

    val payload: Payload
}

fun <P, E> ValidationSystemBuilder<P, E>.checkForFeeChanges(
    calculateFee: suspend (P) -> Fee,
    currentFee: (P) -> BigDecimal,
    chainAsset: (P) -> Chain.Asset,
    error: (FeeChangeDetectedFailure.Payload) -> E
) = validate(
    FeeChangeValidation(
        calculateFee = { payload ->
            val newFee = calculateFee(payload)

            DecimalFee(
                fee = newFee,
                decimalAmount =  chainAsset(payload).amountFromPlanks(newFee.amount)
            )

        },
        currentFee = currentFee,
        error = error,
        chainAsset = chainAsset
    )
)

fun CoroutineScope.handleFeeSpikeDetected(
    error: FeeChangeDetectedFailure,
    resourceManager: ResourceManager,
    feeLoaderMixin: FeeLoaderMixin.Presentation,
    actions: ValidationFlowActions
): TransformedFailure? {
    if (!error.payload.needsUserAttention) {
        actions.resumeFlow()
        return null
    }

    val chainAsset = error.payload.chainAsset
    val oldFee = error.payload.oldFee.formatTokenAmount(chainAsset)
    val newFee = error.payload.newFee.decimalAmount.formatTokenAmount(chainAsset)

    return TransformedFailure.Custom(
        Payload(
            title = resourceManager.getString(R.string.common_fee_changed_title),
            message = resourceManager.getString(R.string.common_fee_changed_message, newFee, oldFee),
            customStyle = R.style.AccentNegativeAlertDialogTheme_Reversed,
            okAction = DialogAction(
                title = resourceManager.getString(R.string.common_proceed),
                action = actions::resumeFlow
            ),
            cancelAction = DialogAction(
                title = resourceManager.getString(R.string.common_refresh_fee),
                action = {
                    launch {
                        feeLoaderMixin.setFee(error.payload.newFee.fee)
                    }
                }
            )
        )
    )
}
