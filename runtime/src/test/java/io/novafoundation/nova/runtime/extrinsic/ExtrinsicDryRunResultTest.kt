package io.novafoundation.nova.runtime.extrinsic

import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import io.novasama.substrate_sdk_android.runtime.definitions.dynamic.DynamicTypeResolver
import io.novasama.substrate_sdk_android.runtime.definitions.registry.TypeRegistry
import io.novasama.substrate_sdk_android.runtime.definitions.types.TypeReference
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Null

class ExtrinsicDryRunResultTest {

    private val runtime = mock(RuntimeSnapshot::class.java)

    @Test
    fun `only exact outer and inner success is accepted`() {
        assertEquals(ExtrinsicDryRunResult.Success, decodeExtrinsicDryRunResult("0x0000", runtime))
        for (response in listOf("", "0x", "0x00", "0x000000", "0x02", "0xGG", "0x01", "0x0001")) {
            assertTrue(response, runCatching { decodeExtrinsicDryRunResult(response, runtime) }.isFailure)
        }
    }

    @Test
    fun `dispatch and validity failures are not outer success`() {
        val nullReference = TypeReference(Null)
        val dispatch = DictEnum("DispatchError", listOf(DictEnum.Entry("Token", nullReference)))
        val invalid = DictEnum("InvalidTransaction", listOf(DictEnum.Entry("BadProof", nullReference)))
        val invalidReference = TypeReference(invalid)
        val validity = DictEnum("TransactionValidityError", listOf(DictEnum.Entry("Invalid", invalidReference)))
        val resolver = DynamicTypeResolver()
        val registry = TypeRegistry(
            mapOf("DispatchError" to TypeReference(dispatch), "TransactionValidityError" to TypeReference(validity)),
            resolver
        )
        `when`(runtime.typeRegistry).thenReturn(registry)

        assertEquals(ExtrinsicDryRunResult.DispatchFailure("Not enough tokens"), decodeExtrinsicDryRunResult("0x000100", runtime))
        assertEquals(ExtrinsicDryRunResult.InvalidTransaction("Invalid.BadProof"), decodeExtrinsicDryRunResult("0x010000", runtime))
    }
}
