package io.novafoundation.nova.feature_wallet_impl.domain.validaiton.multisig

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.callHash
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isFalseOrError
import io.novafoundation.nova.feature_account_api.data.multisig.repository.MultisigValidationsRepository
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidation
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidationFailure
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidationPayload
import io.novafoundation.nova.feature_account_api.data.multisig.validation.multisigAccountId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getRuntime

class MultisigNoPendingCallHashValidation(
    private val chainRegistry: ChainRegistry,
    private val multisigValidationsRepository: MultisigValidationsRepository,
): MultisigExtrinsicValidation {

    override suspend fun validate(value: MultisigExtrinsicValidationPayload): ValidationStatus<MultisigExtrinsicValidationFailure> {
        val runtime = chainRegistry.getRuntime(value.chain.id)
        val callHash = value.delegatedCall.callHash(runtime).intoKey()

        val hasPendingCallHash = multisigValidationsRepository.hasPendingCallHash(value.chain.id, value.multisigAccountId(), callHash)

        return hasPendingCallHash isFalseOrError {
            MultisigExtrinsicValidationFailure.OperationAlreadyExists(value.multisig)
        }
    }
}
