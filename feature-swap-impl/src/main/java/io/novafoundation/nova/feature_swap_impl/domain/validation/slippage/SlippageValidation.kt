package io.novafoundation.nova.feature_swap_impl.domain.validation.slippage

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.common.validation.validationWarning

class SlippageValidation(
    private val minSlippage: Percent,
    private val maxSlippage: Percent,
    private val smallSlippage: Percent,
    private val bigSlippage: Percent
) : Validation<SlippageValidationPayload, SlippageValidationFailure> {

    override suspend fun validate(value: SlippageValidationPayload): ValidationStatus<SlippageValidationFailure> {
        if (value.slippage < minSlippage || value.slippage > maxSlippage) {
            return SlippageValidationFailure.NotInAvailableRange(minSlippage, maxSlippage).validationError()
        }

        if (value.slippage < smallSlippage) {
            return SlippageValidationFailure.TooSmall.validationWarning()
        }

        if (value.slippage > bigSlippage) {
            return SlippageValidationFailure.TooBig.validationWarning()
        }

        return ValidationStatus.Valid()
    }
}
