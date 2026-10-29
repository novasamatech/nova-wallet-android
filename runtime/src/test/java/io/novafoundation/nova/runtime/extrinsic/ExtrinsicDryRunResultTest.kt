package io.novafoundation.nova.runtime.extrinsic

import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Before
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import io.novasama.substrate_sdk_android.runtime.definitions.dynamic.DynamicTypeResolver
import io.novasama.substrate_sdk_android.runtime.definitions.registry.TypeRegistry
import io.novasama.substrate_sdk_android.runtime.definitions.types.TypeReference
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Null

class ExtrinsicDryRunResultTest {

    private val runtime = mock(RuntimeSnapshot::class.java)

    @Before
    fun setUp() {
        val nullReference = TypeReference(Null)
        val dispatchErrorType = DictEnum("DispatchError", listOf(DictEnum.Entry("Token", nullReference)))
        val invalidTransactionType = DictEnum("InvalidTransaction", listOf(DictEnum.Entry("BadProof", nullReference)))
        val invalidTransactionReference = TypeReference(invalidTransactionType)
        val transactionValidityType = DictEnum("TransactionValidityError", listOf(DictEnum.Entry("Invalid", invalidTransactionReference)))
        val dispatchErrorReference = TypeReference(dispatchErrorType)
        val transactionValidityReference = TypeReference(transactionValidityType)
        val resolver = DynamicTypeResolver()
        val registry = TypeRegistry(
            mapOf("DispatchError" to dispatchErrorReference, "TransactionValidityError" to transactionValidityReference),
            resolver
        )
        `when`(runtime.typeRegistry).thenReturn(registry)
    }

    @Test
    fun `only exact outer and inner success is accepted`() {
        assertEquals(ExtrinsicDryRunResult.Success, decodeExtrinsicDryRunResult("0x0000", runtime))
        for (response in listOf("", "0x", "0x00", "0x000000", "0x02", "0xGG", "0x01", "0x0001")) {
            assertTrue(
                response,
                runCatching { decodeExtrinsicDryRunResult(response, runtime) }.isFailure
            )
        }
    }

    @Test
    fun `dispatch and validity failures are not outer success`() {
        assertEquals(ExtrinsicDryRunResult.DispatchFailure("Not enough tokens"), decodeExtrinsicDryRunResult("0x000100", runtime))
        assertEquals(ExtrinsicDryRunResult.InvalidTransaction("Invalid.BadProof"), decodeExtrinsicDryRunResult("0x010000", runtime))
    }

    @Test
    fun `trailing bytes cannot be accepted as a decoded failure`() {
        for (response in listOf("0x00010000", "0x01000000")) {
            val error = runCatching { decodeExtrinsicDryRunResult(response, runtime) }.exceptionOrNull()

            assertTrue(response, error is IllegalArgumentException)
            assertEquals("Trailing bytes in dry-run response", error?.message)
        }
    }

    @Test
    fun `unknown result discriminants and malformed errors are rejected`() {
        for (response in listOf("0x0002", "0x02", "0x000101", "0x010100", "0x0100")) {
            assertTrue(
                response,
                runCatching { decodeExtrinsicDryRunResult(response, runtime) }.isFailure
            )
        }
    }
}
