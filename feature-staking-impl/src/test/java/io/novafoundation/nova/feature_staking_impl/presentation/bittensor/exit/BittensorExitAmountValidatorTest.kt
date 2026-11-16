package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.feature_staking_impl.R
import java.math.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorExitAmountValidatorTest {

    private val resources = mock(ResourceManager::class.java)

    private val precisionValidator = BittensorExitPrecisionValidator(resources)

    private val validator = BittensorExitAmountValidator(resources, precisionValidator)

    private val precision = MutableStateFlow<Int?>(9)

    private val maximum = MutableStateFlow<BigDecimal?>(BigDecimal.ONE)

    private val input = MutableStateFlow("")

    private val fieldValidator = validator.forAmount(precision, maximum)

    @Before
    fun setUp() {
        `when`(resources.getString(R.string.common_error_not_enough_tokens)).thenReturn("Not enough tokens")
        `when`(resources.getString(R.string.bittensor_amount_precision_error, 9)).thenReturn("Use 9 decimal places")
    }

    @Test
    fun `exact maximum is valid and one plank above reports insufficient tokens`() = runTest {
        val result = observeValidation()
        input.value = "1.000000000"
        runCurrent()
        assertSame(FieldValidationResult.Ok, result.value)

        input.value = "1.000000001"
        runCurrent()
        assertError("Not enough tokens", result.value)
    }

    @Test
    fun `editing below maximum and clearing recover from overflow`() = runTest {
        val result = observeValidation()
        input.value = "2"
        runCurrent()
        assertError("Not enough tokens", result.value)

        input.value = "0.5"
        runCurrent()
        assertSame(FieldValidationResult.Ok, result.value)

        input.value = "2"
        runCurrent()
        assertError("Not enough tokens", result.value)
        input.value = ""
        runCurrent()
        assertSame(FieldValidationResult.Ok, result.value)
    }

    @Test
    fun `precision error takes precedence and harmless trailing zeros remain valid`() = runTest {
        val result = observeValidation()
        input.value = "1.0000000001"
        runCurrent()
        assertError("Use 9 decimal places", result.value)

        input.value = "1.0000000000"
        runCurrent()
        assertSame(FieldValidationResult.Ok, result.value)
    }

    @Test
    fun `unknown maximum never invents insufficient balance and clears a previous range error`() = runTest {
        val result = observeValidation()
        input.value = "2"
        runCurrent()
        assertError("Not enough tokens", result.value)

        maximum.value = null
        runCurrent()
        assertSame(FieldValidationResult.Ok, result.value)

        input.value = "999999999999999999999"
        runCurrent()
        assertSame(FieldValidationResult.Ok, result.value)
    }

    @Test
    fun `zero maximum rejects positive input but accepts blank and zero`() = runTest {
        maximum.value = BigDecimal.ZERO
        val result = observeValidation()
        input.value = "0.000000001"
        runCurrent()
        assertError("Not enough tokens", result.value)

        for (text in listOf("", "0", "0.0000000000")) {
            input.value = text
            runCurrent()
            assertSame(FieldValidationResult.Ok, result.value)
        }
    }

    @Test
    fun `live maximum changes revalidate unchanged input`() = runTest {
        input.value = "0.75"
        val result = observeValidation()
        assertSame(FieldValidationResult.Ok, result.value)

        maximum.value = BigDecimal("0.749999999")
        runCurrent()
        assertError("Not enough tokens", result.value)

        maximum.value = BigDecimal("0.75")
        runCurrent()
        assertSame(FieldValidationResult.Ok, result.value)
    }

    @Test
    fun `loaded precision revalidates existing input independently of unknown maximum`() = runTest {
        maximum.value = null
        precision.value = null
        input.value = "0.0000000001"
        val result = observeValidation()
        assertSame(FieldValidationResult.Ok, result.value)

        precision.value = 9
        runCurrent()
        assertError("Use 9 decimal places", result.value)

        precision.value = 12
        runCurrent()
        assertSame(FieldValidationResult.Ok, result.value)
    }

    private fun TestScope.observeValidation(): StateFlow<FieldValidationResult> {
        val result = fieldValidator.observe(input).stateIn(backgroundScope, SharingStarted.Eagerly, FieldValidationResult.Ok)
        runCurrent()
        return result
    }

    private fun assertError(reason: String, result: FieldValidationResult) {
        assertTrue(result is FieldValidationResult.Error)
        assertEquals(reason, (result as FieldValidationResult.Error).reason)
    }
}
