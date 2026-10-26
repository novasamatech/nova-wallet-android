package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class BittensorExitPrecisionValidatorTest {

    private val resources = mock(ResourceManager::class.java)

    private val validator = BittensorExitPrecisionValidator(resources)

    @Test
    fun `nonzero sub-plank fraction reports standard field error without rounding`() {
        `when`(resources.getString(R.string.bittensor_amount_precision_error, 9)).thenReturn("Use no more than 9 decimal places.")
        val result = validator.validate("1.0000000001", 9)
        assertTrue(result is FieldValidationResult.Error)
        assertEquals("Use no more than 9 decimal places.", (result as FieldValidationResult.Error).reason)
    }

    @Test
    fun `exact plank and harmless trailing zeros remain valid`() {
        for (input in listOf("", "0", "0.000000001", "1.123456789", "1.0000000000")) {
            assertSame(FieldValidationResult.Ok, validator.validate(input, 9))
        }
    }

    @Test
    fun `loaded asset precision revalidates existing text`() = runBlocking {
        `when`(resources.getString(R.string.bittensor_amount_precision_error, 9)).thenReturn("precision")
        val precision = flowOf<Int?>(9)
        val fieldValidator = validator.forPrecision(precision)
        val input = flowOf("0.0000000001")
        val results = fieldValidator.observe(input).toList()
        assertTrue(results.single() is FieldValidationResult.Error)
        assertSame(FieldValidationResult.Ok, validator.validate("0.000000001", 9))
        assertSame(FieldValidationResult.Ok, validator.validate("0.0000000001", 12))
    }
}
