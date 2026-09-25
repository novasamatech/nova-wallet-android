package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.registry.TypeRegistry
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.FixedByteArray
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.u16
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.u64
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.BooleanType
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import io.novasama.substrate_sdk_android.runtime.definitions.types.useScaleWriter
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.metadata.ExtrinsicMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.FunctionArgument
import io.novasama.substrate_sdk_android.runtime.metadata.module.MetadataFunction
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigInteger

class BittensorStakingCallsTest {

    @Test
    fun `root stake encodes account u16 netuid and u64 amount in runtime order`() {
        val function = MetadataFunction(
            "add_stake",
            listOf(
                FunctionArgument("hotkey", FixedByteArray("AccountId32", 32)),
                FunctionArgument("netuid", u16),
                FunctionArgument("amount_staked", u64)
            ),
            emptyList(),
            7 to 2
        )
        val module = Module("SubtensorModule", null, mapOf("add_stake" to function), null, emptyMap(), emptyMap(), BigInteger.valueOf(7))
        val runtime = RuntimeSnapshot(
            mock(TypeRegistry::class.java),
            RuntimeMetadata(15, mapOf(module.name to module), ExtrinsicMetadata(BigInteger.valueOf(4), emptyList()), emptyList())
        )
        val builder = mock(ExtrinsicBuilder::class.java)
        `when`(builder.runtime).thenReturn(runtime)
        val hotkey = ByteArray(32) { 42 }

        builder.addBittensorRootStake(hotkey, BigInteger.TEN)
        val invocation = mockingDetails(builder).invocations.single { it.method.name == "call" }
        val call = invocation.getArgument<GenericCall.Instance>(0)
        val encoded = useScaleWriter { GenericCall.encode(this, runtime, call) }

        assertArrayEquals(byteArrayOf(7, 2) + hotkey + byteArrayOf(0, 0, 10, 0, 0, 0, 0, 0, 0, 0), encoded)
    }

    @Test
    fun `invalid amounts fail before reading runtime or creating calls`() {
        val builder = mock(ExtrinsicBuilder::class.java)
        val invalidAmounts = listOf(BigInteger.ZERO, BigInteger.valueOf(-1), BigInteger.ONE.shiftLeft(64))

        invalidAmounts.forEach { amount ->
            val failure = runCatching { builder.addBittensorRootStake(ByteArray(32), amount) }.exceptionOrNull()
            assertTrue(failure is IllegalArgumentException)
        }
        verifyNoInteractions(builder)
    }

    @Test
    fun `subnet buy and sell encode exact amounts and disallow partial execution`() {
        for (buying in listOf(true, false)) {
            val name = if (buying) "add_stake_limit" else "remove_stake_limit"
            val amountArgument = if (buying) "amount_staked" else "amount_unstaked"
            val function = MetadataFunction(
                name,
                listOf(
                    FunctionArgument("hotkey", FixedByteArray("AccountId32", 32)),
                    FunctionArgument("netuid", u16),
                    FunctionArgument(amountArgument, u64),
                    FunctionArgument("limit_price", u64),
                    FunctionArgument("allow_partial", BooleanType)
                ),
                emptyList(),
                7 to 3
            )
            val module = Module("SubtensorModule", null, mapOf(name to function), null, emptyMap(), emptyMap(), BigInteger.valueOf(7))
            val runtime = RuntimeSnapshot(
                mock(TypeRegistry::class.java),
                RuntimeMetadata(15, mapOf(module.name to module), ExtrinsicMetadata(BigInteger.valueOf(4), emptyList()), emptyList())
            )
            val builder = mock(ExtrinsicBuilder::class.java)
            `when`(builder.runtime).thenReturn(runtime)
            val hotkey = ByteArray(32) { 42 }
            if (buying) {
                builder.addBittensorSubnetStake(BittensorSubnetId(64), hotkey, BigInteger.TEN, BigInteger.valueOf(100))
            } else {
                builder.removeBittensorSubnetStake(BittensorSubnetId(64), hotkey, BigInteger.TEN, BigInteger.valueOf(100))
            }
            val invocation = mockingDetails(builder).invocations.single { it.method.name == "call" }
            val call = invocation.getArgument<GenericCall.Instance>(0)
            val encoded = useScaleWriter { GenericCall.encode(this, runtime, call) }

            assertArrayEquals(
                byteArrayOf(7, 3) + hotkey + byteArrayOf(64, 0) +
                    byteArrayOf(10, 0, 0, 0, 0, 0, 0, 0) + byteArrayOf(100, 0, 0, 0, 0, 0, 0, 0) + byteArrayOf(0),
                encoded
            )
        }
    }

    @Test
    fun `invalid subnet call parameters are rejected before creating a call`() {
        val builder = mock(ExtrinsicBuilder::class.java)
        val subnetId = BittensorSubnetId(64)
        assertTrue(runCatching { builder.addBittensorSubnetStake(subnetId, ByteArray(31), BigInteger.TEN, BigInteger.TEN) }.isFailure)
        assertTrue(runCatching { builder.removeBittensorSubnetStake(subnetId, ByteArray(32), BigInteger.ZERO, BigInteger.TEN) }.isFailure)
        assertTrue(runCatching { builder.addBittensorSubnetStake(subnetId, ByteArray(32), BigInteger.TEN, BigInteger.ZERO) }.isFailure)
        assertTrue(runCatching { builder.removeBittensorSubnetStake(subnetId, ByteArray(32), BigInteger.ONE.shiftLeft(64), BigInteger.TEN) }.isFailure)
        verifyNoInteractions(builder)
    }
}
