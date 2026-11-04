package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.CompoundFieldValidator
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.common.validation.FieldValidator
import io.novafoundation.nova.feature_staking_impl.R
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class BittensorExitAmountValidator @Inject constructor(
    private val resources: ResourceManager,
    private val precisionValidator: BittensorExitPrecisionValidator
) {

    fun forAmount(precision: Flow<Int?>, maximum: Flow<BigDecimal?>): FieldValidator {
        val precisionFieldValidator = precisionValidator.forPrecision(precision)
        val maximumFieldValidator = object : FieldValidator {
            override fun observe(inputStream: Flow<String>): Flow<FieldValidationResult> =
                combine(inputStream, maximum) { input, maximum ->
                    val amount = input.toBigDecimalOrNull()
                    if (amount != null && maximum != null && amount > maximum) {
                        FieldValidationResult.Error(resources.getString(R.string.common_error_not_enough_tokens))
                    } else {
                        FieldValidationResult.Ok
                    }
                }
        }
        return CompoundFieldValidator(precisionFieldValidator, maximumFieldValidator)
    }
}
