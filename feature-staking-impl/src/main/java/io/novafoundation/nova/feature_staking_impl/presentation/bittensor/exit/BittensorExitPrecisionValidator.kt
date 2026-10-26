package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.common.validation.FieldValidator
import io.novafoundation.nova.feature_staking_impl.R
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class BittensorExitPrecisionValidator @Inject constructor(private val resources: ResourceManager) {

    fun forPrecision(precision: Flow<Int?>): FieldValidator = object : FieldValidator {
        override fun observe(inputStream: Flow<String>): Flow<FieldValidationResult> =
            combine(inputStream, precision) { input, decimals -> validate(input, decimals) }
    }

    internal fun validate(input: String, precision: Int?): FieldValidationResult {
        val amount = input.toBigDecimalOrNull() ?: return FieldValidationResult.Ok
        if (precision == null || amount.stripTrailingZeros().scale() <= precision) return FieldValidationResult.Ok
        return FieldValidationResult.Error(resources.getString(R.string.bittensor_amount_precision_error, precision))
    }
}
