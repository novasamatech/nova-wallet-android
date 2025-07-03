package io.novafoundation.nova.feature_multisig_operations.domain.details.validations

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.feature_account_api.data.multisig.repository.MultisigValidationsRepository
import javax.inject.Inject

@FeatureScope
class OperationIsStillPendingValidation @Inject constructor(
    private val multisigValidationsRepository: MultisigValidationsRepository
) : ApproveMultisigOperationValidation {

    override suspend fun validate(value: ApproveMultisigOperationValidationPayload): ValidationStatus<ApproveMultisigOperationValidationFailure> {
        val hasPendingCallHash = multisigValidationsRepository.hasPendingCallHash(value.chain.id, value.signatoryAccountId, value.operation.callHash)

        return hasPendingCallHash isTrueOrError {
            ApproveMultisigOperationValidationFailure.TransactionIsNotAvailable
        }
    }
}
