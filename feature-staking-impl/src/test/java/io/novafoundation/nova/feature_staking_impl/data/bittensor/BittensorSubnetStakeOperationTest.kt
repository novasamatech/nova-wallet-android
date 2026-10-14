package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetBuyQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSwapDirection
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSwapSimulation
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.dynamic.DynamicTypeResolver
import io.novasama.substrate_sdk_android.runtime.definitions.registry.TypeRegistry
import io.novasama.substrate_sdk_android.runtime.definitions.types.TypeReference
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.BooleanType
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.FixedByteArray
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.u16
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.u64
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.metadata.ExtrinsicMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.FunctionArgument
import io.novasama.substrate_sdk_android.runtime.metadata.module.MetadataFunction
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.`when`
import java.math.BigInteger

class BittensorSubnetStakeOperationTest {

    private val hotkey = AccountIdKey(ByteArray(32) { 1 })
    private val beneficiary = AccountIdKey(ByteArray(32) { 2 })

    @Test
    fun `builds stake first and fee transfer second with exact confirmed amounts`() {
        val builder = builder()
        val quote = quote(BigInteger.valueOf(3))
        BittensorSubnetStakeOperation(quote, hotkey, beneficiary).build(builder)
        val calls = calls(builder)
        assertEquals(2, calls.size)
        assertEquals("add_stake_limit", calls[0].function.name)
        assertEquals(BigInteger.valueOf(997), calls[0].arguments["amount_staked"])
        assertEquals(quote.limitPrice, calls[0].arguments["limit_price"])
        assertEquals(false, calls[0].arguments["allow_partial"])
        assertEquals("transfer_keep_alive", calls[1].function.name)
        assertEquals(BigInteger.valueOf(3), calls[1].arguments["value"])
        assertArrayEquals(beneficiary.value, calls[1].arguments["dest"] as ByteArray)
    }

    @Test
    fun `net simulation input plus runtime fee is a complete stake`() {
        val builder = builder()
        val quote = quote(BigInteger.valueOf(3))
        val runtimeFee = BigInteger.valueOf(2)
        val netSimulation = quote.simulation.copy(amountIn = quote.stakeTaoInPlanks - runtimeFee, taoFee = runtimeFee)
        val operation = BittensorSubnetStakeOperation(quote.copy(simulation = netSimulation), hotkey, beneficiary)
        operation.build(builder)
        assertEquals(quote.stakeTaoInPlanks, calls(builder).first().arguments["amount_staked"])
    }

    @Test
    fun `zero rounded commission does not emit a zero value transfer`() {
        val builder = builder()
        BittensorSubnetStakeOperation(quote(BigInteger.ZERO), hotkey, beneficiary).build(builder)
        assertEquals(1, calls(builder).size)
    }

    @Test
    fun `a partial simulation or invalid recipient cannot become an operation`() {
        val quote = quote(BigInteger.valueOf(3))
        assertTrue(
            runCatching {
                BittensorSubnetStakeOperation(quote.copy(serviceFeeTaoInPlanks = BigInteger.valueOf(4)), hotkey, beneficiary)
            }.isFailure
        )
        assertTrue(
            runCatching {
                BittensorSubnetStakeOperation(quote, hotkey, AccountIdKey(ByteArray(31)))
            }.isFailure
        )
    }

    private fun quote(fee: BigInteger): BittensorSubnetBuyQuote {
        val subnet = BittensorSubnetId(64)
        return BittensorSubnetBuyQuote(
            subnet,
            BigInteger.valueOf(1000),
            fee,
            BittensorSwapSimulation(
                subnet,
                BittensorSwapDirection.TAO_TO_ALPHA,
                BigInteger.valueOf(1000) - fee,
                BigInteger.TEN,
                BigInteger.ZERO,
                BigInteger.ZERO,
                BigInteger.ZERO,
                BigInteger.ZERO
            ),
            BigInteger.valueOf(1005),
            java.math.BigDecimal("0.003")
        )
    }

    private fun calls(builder: ExtrinsicBuilder): List<GenericCall.Instance> = mockingDetails(builder).invocations
        .filter { it.method.name == "call" }.map { it.getArgument(0) }

    private fun builder(): ExtrinsicBuilder {
        val account = FixedByteArray("AccountId32", 32)
        val stake = MetadataFunction(
            "add_stake_limit",
            listOf(
                FunctionArgument("hotkey", account),
                FunctionArgument("netuid", u16),
                FunctionArgument("amount_staked", u64),
                FunctionArgument("limit_price", u64),
                FunctionArgument("allow_partial", BooleanType)
            ),
            emptyList(),
            7 to 3
        )
        val transfer = MetadataFunction(
            "transfer_keep_alive",
            listOf(FunctionArgument("dest", account), FunctionArgument("value", u64)),
            emptyList(),
            5 to 3
        )
        val modules = listOf(
            Module("SubtensorModule", null, mapOf(stake.name to stake), null, emptyMap(), emptyMap(), BigInteger.valueOf(7)),
            Module("Balances", null, mapOf(transfer.name to transfer), null, emptyMap(), emptyMap(), BigInteger.valueOf(5))
        ).associateBy { it.name }
        val runtime = RuntimeSnapshot(
            TypeRegistry(mapOf("Address" to TypeReference(account)), DynamicTypeResolver()),
            RuntimeMetadata(15, modules, ExtrinsicMetadata(BigInteger.valueOf(4), emptyList()), emptyList())
        )
        return mock(ExtrinsicBuilder::class.java).also { `when`(it.runtime).thenReturn(runtime) }
    }
}
