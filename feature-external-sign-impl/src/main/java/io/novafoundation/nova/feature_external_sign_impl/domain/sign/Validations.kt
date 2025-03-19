package io.novafoundation.nova.feature_external_sign_impl.domain.sign

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.domain.validation.FeeChangeDetectedFailure

sealed class ConfirmDAppOperationValidationFailure {

    class FeeSpikeDetected(override val payload: FeeChangeDetectedFailure.Payload<Fee>) :
        ConfirmDAppOperationValidationFailure(),
        FeeChangeDetectedFailure<Fee>
}

data class ConfirmDAppOperationValidationPayload(
    val fee: Fee?
)

typealias ConfirmDAppOperationValidationSystem = ValidationSystem<ConfirmDAppOperationValidationPayload, ConfirmDAppOperationValidationFailure>
