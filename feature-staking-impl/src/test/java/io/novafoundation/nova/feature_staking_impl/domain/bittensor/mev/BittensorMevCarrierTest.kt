package io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicBuildingContext
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.signer.NovaSigner
import io.novafoundation.nova.feature_staking_impl.data.bittensor.mev.BittensorMevExtrinsics
import io.novafoundation.nova.feature_staking_impl.data.bittensor.mev.BittensorMevFeeSignerFactory
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.dynamic.DynamicTypeResolver
import io.novasama.substrate_sdk_android.runtime.definitions.registry.TypeRegistry
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.FixedByteArray
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.metadata.ExtrinsicMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.FunctionArgument
import io.novasama.substrate_sdk_android.runtime.metadata.module.MetadataFunction
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.`when`

class BittensorMevCarrierTest {

    private val builders = mock(ExtrinsicBuilderFactory::class.java)

    private val rpc = mock(RpcCalls::class.java)

    private val feeSigners = BittensorMevFeeSignerFactory()

    private val extrinsics = BittensorMevExtrinsics(builders, rpc, feeSigners)

    private val signer = mock(NovaSigner::class.java)

    private val chain = mock(Chain::class.java)

    @Test
    fun `the carrier contains just the top level encrypted call and no clear order`() = runTest {
        val payload = byteArrayOf(1, 2, 3)
        val function = MetadataFunction(
            "submit_encrypted",
            listOf(FunctionArgument("ciphertext", FixedByteArray("Ciphertext", 3))),
            emptyList(),
            20 to 0
        )
        val module = Module("MevShield", null, mapOf(function.name to function), null, emptyMap(), emptyMap(), 20.toBigInteger())
        val registry = TypeRegistry(emptyMap(), DynamicTypeResolver())
        val runtime = RuntimeSnapshot(
            registry,
            RuntimeMetadata(15, mapOf("MevShield" to module), ExtrinsicMetadata(4.toBigInteger(), emptyList()), emptyList())
        )
        val builder = mock(ExtrinsicBuilder::class.java)
        `when`(builder.runtime).thenReturn(runtime)
        val origin = SubmissionOrigin(ByteArray(32), ByteArray(32))
        val context = ExtrinsicBuildingContext(origin, signer, chain)
        val carrier = extrinsics.carrier(payload)
        builder.carrier(context)
        val calls = mockingDetails(builder).invocations.filter { it.method.name == "call" }.map { it.getArgument<GenericCall.Instance>(0) }
        assertEquals(1, calls.size)
        assertEquals("MevShield", calls.single().module.name)
        assertEquals("submit_encrypted", calls.single().function.name)
        assertArrayEquals(payload, calls.single().arguments["ciphertext"] as ByteArray)
    }
}
