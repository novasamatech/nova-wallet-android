package io.novafoundation.nova.app.root.presentation.requestBusHandler

import io.novafoundation.nova.common.utils.bus.observeBusEvent
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidationRequestBus
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidationRequestBus.ValidationResponse
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidationSystem
import io.novafoundation.nova.feature_wallet_api.domain.validation.MultisigExtrinsicValidationFactory
import kotlinx.coroutines.flow.Flow

class MultisigExtrinsicValidationRequestBusHandler(
    private val multisigExtrinsicValidationRequestBus: MultisigExtrinsicValidationRequestBus,
    private val multisigExtrinsicValidationFactory: MultisigExtrinsicValidationFactory
) : RequestBusHandler {

    override fun observe(): Flow<*> {
        return multisigExtrinsicValidationRequestBus.observeEvent()
            .observeBusEvent { request ->
                val validationResult = createValidationSystem()
                    .validate(request.validationPayload)

                ValidationResponse(validationResult)
            }
    }

    private fun createValidationSystem(): MultisigExtrinsicValidationSystem {
        return ValidationSystem {
            multisigExtrinsicValidationFactory.multisigSignatoryHasEnoughBalance()
            multisigExtrinsicValidationFactory.noPendingMultisigWithSameCallData()
        }
    }
}
