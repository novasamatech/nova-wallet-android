package io.novafoundation.nova.feature_account_api.data.multisig.validation

import io.novafoundation.nova.common.utils.bus.BaseRequestBus
import io.novafoundation.nova.common.utils.bus.RequestBus
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidationRequestBus.Request
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidationRequestBus.ValidationResponse

class MultisigExtrinsicValidationRequestBus() : BaseRequestBus<Request, ValidationResponse>() {

    class Request(val validationPayload: MultisigExtrinsicValidationPayload) : RequestBus.Request

    class ValidationResponse(val validationResult: Result<ValidationStatus<MultisigExtrinsicValidationFailure>>) : RequestBus.Response
}
