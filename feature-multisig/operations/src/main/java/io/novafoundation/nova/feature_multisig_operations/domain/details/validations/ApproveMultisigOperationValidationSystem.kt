package io.novafoundation.nova.feature_multisig_operations.domain.details.validations

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.countedTowardsEdAmount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.transferableAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.feature_wallet_api.domain.validation.validate
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset

typealias ApproveMultisigOperationValidationSystem = ValidationSystem<ApproveMultisigOperationValidationPayload, ApproveMultisigOperationValidationFailure>
typealias ApproveMultisigOperationValidation = Validation<ApproveMultisigOperationValidationPayload, ApproveMultisigOperationValidationFailure>
typealias ApproveMultisigOperationValidationSystemBuilder =
    ValidationSystemBuilder<ApproveMultisigOperationValidationPayload, ApproveMultisigOperationValidationFailure>

fun ValidationSystem.Companion.approveMultisigOperation(
    edFactory: EnoughTotalToStayAboveEDValidationFactory,
    operationStillPendingValidation: OperationIsStillPendingValidation,
): ApproveMultisigOperationValidationSystem = ValidationSystem {
    enoughToPayFeesAndStayAboveEd(edFactory)

    enoughToPayFees()

    validate(operationStillPendingValidation)
}

private fun ApproveMultisigOperationValidationSystemBuilder.enoughToPayFees() {
    sufficientBalance(
        fee = { it.fee },
        available = { it.signatoryBalance.transferableAmount() },
        error = {
            ApproveMultisigOperationValidationFailure.NotEnoughBalanceToPayFees(
                signatory = it.payload.signatory,
                chainAsset = it.payload.signatoryBalance.chainAsset,
                minimumNeeded = it.fee,
                available = it.payload.signatoryBalance.transferableAmount()
            )
        }
    )
}

private fun ApproveMultisigOperationValidationSystemBuilder.enoughToPayFeesAndStayAboveEd(edFactory: EnoughTotalToStayAboveEDValidationFactory) {
    edFactory.validate(
        fee = { it.fee },
        balance = { it.signatoryBalance.countedTowardsEdAmount() },
        chainWithAsset = { ChainWithAsset(it.chain, it.signatoryBalance.chainAsset) },
        error = { payload, errorModel ->
            ApproveMultisigOperationValidationFailure.NotEnoughBalanceToPayFees(
                signatory = payload.signatory,
                chainAsset = payload.signatoryBalance.chainAsset,
                minimumNeeded = errorModel.minRequiredBalance,
                available = errorModel.availableBalance
            )
        }
    )
}
