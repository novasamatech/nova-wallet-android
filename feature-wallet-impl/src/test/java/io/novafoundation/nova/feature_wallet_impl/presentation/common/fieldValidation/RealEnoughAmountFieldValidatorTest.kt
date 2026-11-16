package io.novafoundation.nova.feature_wallet_impl.presentation.common.fieldValidation

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Precision
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.presentation.common.fieldValidator.EnoughAmountFieldValidator
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxAvailableBalance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class RealEnoughAmountFieldValidatorTest {

    private val resources = Mockito.mock(ResourceManager::class.java)

    private val asset = Mockito.mock(Chain.Asset::class.java)

    // The label may show the full balance; validation must use the spendable amount after all deductions.
    private val available = MutableStateFlow(MaxAvailableBalance(asset, BigInteger("60490000"), BigInteger("49450000")))

    private val maxProvider = object : MaxActionProvider {
        override val maxAvailableBalance = available
    }

    private val factory = RealEnoughAmountValidatorFactory(resources)

    private val validator = factory.create(maxProvider)

    @Before
    fun setUp() {
        Mockito.`when`(asset.precision).thenReturn(Precision(9))
        Mockito.`when`(resources.getString(R.string.common_error_not_enough_tokens)).thenReturn("Not enough tokens")
    }

    @Test
    fun `oversized amount and an amount inside the fee reserve fail against actual Max`() = runBlocking {
        for (input in listOf("50.03", "0.06049", "0.05", "0.049450001")) {
            val result = validator.observe(flowOf(input)).first()
            assertTrue("Expected an error for $input", result is FieldValidationResult.Error)
            val error = result as FieldValidationResult.Error
            assertEquals(EnoughAmountFieldValidator.ERROR_TAG, error.tag)
            assertEquals("Not enough tokens", error.reason)
        }
    }

    @Test
    fun `exact actual Max and one plank below are valid`() = runBlocking {
        for (input in listOf("0.04945", "0.049449999", "0", "")) {
            assertEquals(FieldValidationResult.Ok, validator.observe(flowOf(input)).first())
        }
    }

    @Test
    fun `zero spendable balance rejects a positive amount while empty input stays empty`() = runBlocking {
        available.value = MaxAvailableBalance.fromSingle(asset, BigInteger.ZERO)
        assertTrue(validator.observe(flowOf("0.000000001")).first() is FieldValidationResult.Error)
        assertEquals(FieldValidationResult.Ok, validator.observe(flowOf("")).first())
    }

    @Test
    fun `balance or fee changes revalidate the existing input and editing clears the error`() = runBlocking {
        val input = MutableStateFlow("0.04945")
        val results = Channel<FieldValidationResult>(Channel.UNLIMITED)
        val observation = launch { validator.observe(input).collect { results.send(it) } }
        try {
            withTimeout(5_000) {
                assertEquals(FieldValidationResult.Ok, results.receive())
                available.value = available.value.copy(actualBalance = BigInteger("48450000"))
                assertTrue(results.receive() is FieldValidationResult.Error)
                input.value = "0.04845"
                assertEquals(FieldValidationResult.Ok, results.receive())
                available.value = available.value.copy(actualBalance = BigInteger.ZERO)
                assertTrue(results.receive() is FieldValidationResult.Error)
            }
        } finally {
            observation.cancelAndJoin()
            results.close()
        }
    }
}
