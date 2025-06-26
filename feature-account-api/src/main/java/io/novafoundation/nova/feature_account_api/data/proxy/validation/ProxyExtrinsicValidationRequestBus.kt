package io.novafoundation.nova.feature_account_api.data.proxy.validation

import io.novafoundation.nova.common.utils.bus.BaseRequestBus
import io.novafoundation.nova.common.utils.bus.RequestBus
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxyExtrinsicValidationRequestBus.Request
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxyExtrinsicValidationRequestBus.ValidationResponse

class ProxyExtrinsicValidationRequestBus() : BaseRequestBus<Request, ValidationResponse>() {

    class Request(val validationPayload: ProxiedExtrinsicValidationPayload) : RequestBus.Request

    class ValidationResponse(val validationResult: Result<ValidationStatus<ProxiedExtrinsicValidationFailure>>) : RequestBus.Response
}
