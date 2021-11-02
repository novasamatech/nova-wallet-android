package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.terms

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.custom.moonbeam.MoonbeamTermsValidationFailure

fun moonbeamTermsValidationFailure(
    reason: MoonbeamTermsValidationFailure,
    resourceManager: ResourceManager
): TitleAndMessage {
    return when (reason) {
        MoonbeamTermsValidationFailure.CANNOT_PAY_FEES -> resourceManager.getString(R.string.common_not_enough_funds_title) to
            resourceManager.getString(R.string.common_not_enough_funds_message)
    }
}
