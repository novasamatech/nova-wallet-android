package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitOperation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootClaimOperation
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.dynamic.DynamicTypeResolver
import io.novasama.substrate_sdk_android.runtime.definitions.registry.TypeRegistry
import io.novasama.substrate_sdk_android.runtime.definitions.types.TypeReference
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Option
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.BooleanType
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.FixedByteArray
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.u16
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.u64
import io.novasama.substrate_sdk_android.runtime.definitions.types.useScaleWriter
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.metadata.ExtrinsicMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.module.FunctionArgument
import io.novasama.substrate_sdk_android.runtime.metadata.module.MetadataFunction
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import java.math.BigInteger
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails

class BittensorExitOperationTest {

    private val hotkey = AccountIdKey(ByteArray(32) { 1 })

    private val beneficiary = AccountIdKey(ByteArray(32) { 2 })

    @Test
    fun `partial root withdrawal encodes root netuid and exact unstaked u64 amount`() {
        val builder = builder()
        val quote = BittensorExitQuote(
            BittensorNetuid.from(0),
            BigInteger.TEN,
            false,
            BigInteger.TEN,
            BigInteger.TEN,
            BigInteger.ZERO,
            null
        )
        val operation = BittensorExitOperation(quote, hotkey, beneficiary)
        operation.build(builder)
        val call = calls(builder)
            .single()
        val encoded = useScaleWriter { GenericCall.encode(this, builder.runtime, call) }
        assertArrayEquals(
            byteArrayOf(7, 9) + hotkey.value + byteArrayOf(
                0,
                0,
                10,
                0,
                0,
                0,
                0,
                0,
                0,
                0
            ),
            encoded
        )
    }

    @Test
    fun `root max uses no price limit and emits no service transfer`() {
        val builder = builder()
        val quote = BittensorExitQuote(
            BittensorNetuid.from(0),
            BigInteger.TEN,
            true,
            BigInteger.TEN,
            BigInteger.TEN,
            BigInteger.ZERO,
            null
        )
        val operation = BittensorExitOperation(quote, hotkey, beneficiary)
        operation.build(builder)
        val call = calls(builder)
            .single()
        assertEquals("remove_stake_full_limit", call.function.name)
        val encoded = useScaleWriter { GenericCall.encode(this, builder.runtime, call) }
        assertArrayEquals(byteArrayOf(7, 8) + hotkey.value + byteArrayOf(0, 0, 0), encoded)
    }

    @Test
    fun `subnet sale keeps the confirmed protection and follows it with exact commission`() {
        val builder = builder()
        val quote = BittensorExitQuote(
            BittensorNetuid.Subnet(64),
            BigInteger.valueOf(1000),
            false,
            BigInteger.valueOf(100),
            BigInteger.valueOf(99),
            BigInteger.valueOf(3),
            BigInteger.valueOf(1001)
        )
        val operation = BittensorExitOperation(quote, hotkey, beneficiary)
        operation.build(builder)
        val calls = calls(builder)
        assertEquals(2, calls.size)
        assertEquals(
            "remove_stake_limit",
            calls.first().function.name
        )
        assertEquals(
            quote.amount,
            calls.first().arguments["amount_unstaked"]
        )
        assertEquals(
            quote.limitPrice,
            calls.first().arguments["limit_price"]
        )
        assertEquals(
            false,
            calls.first().arguments["allow_partial"]
        )
        assertEquals(
            "transfer_keep_alive",
            calls.last().function.name
        )
        assertEquals(
            quote.serviceFee,
            calls.last().arguments["value"]
        )
        assertArrayEquals(
            beneficiary.value,
            calls.last().arguments["dest"] as ByteArray
        )
    }

    @Test
    fun `subnet max encodes Some protective price without a partial execution flag`() {
        val builder = builder()
        val quote = BittensorExitQuote(
            BittensorNetuid.Subnet(64),
            BigInteger.valueOf(1000),
            true,
            BigInteger.valueOf(100),
            BigInteger.valueOf(99),
            BigInteger.ZERO,
            BigInteger.valueOf(1001)
        )
        val operation = BittensorExitOperation(quote, hotkey, beneficiary)
        operation.build(builder)
        val call = calls(builder)
            .single()
        val encoded = useScaleWriter { GenericCall.encode(this, builder.runtime, call) }
        assertArrayEquals(
            byteArrayOf(7, 8) + hotkey.value + byteArrayOf(
                64,
                0,
                1,
                -23,
                3,
                0,
                0,
                0,
                0,
                0,
                0
            ),
            encoded
        )
    }

    @Test
    fun `claim only signs the explicit hotkey`() {
        val builder = builder()
        val operation = BittensorRootClaimOperation(hotkey)
        operation.build(builder)
        val call = calls(builder)
            .single()
        val encoded = useScaleWriter { GenericCall.encode(this, builder.runtime, call) }
        assertArrayEquals(byteArrayOf(7, 10) + hotkey.value, encoded)
    }

    private fun calls(builder: ExtrinsicBuilder): List<GenericCall.Instance> = mockingDetails(builder).invocations
        .filter { it.method.name == "call" }
        .map { it.getArgument(0) }

    private fun builder(): ExtrinsicBuilder {
        val account = FixedByteArray("AccountId32", 32)
        val optionalPrice = Option("OptionPrice", TypeReference(u64))
        val common = listOf(FunctionArgument("hotkey", account), FunctionArgument("netuid", u16))
        val root = MetadataFunction(
            "remove_stake",
            common + FunctionArgument("amount_unstaked", u64),
            emptyList(),
            7 to 9
        )
        val sale = MetadataFunction(
            "remove_stake_limit",
            common + listOf(
                FunctionArgument("amount_unstaked", u64),
                FunctionArgument("limit_price", u64),
                FunctionArgument("allow_partial", BooleanType)
            ),
            emptyList(),
            7 to 3
        )
        val full = MetadataFunction(
            "remove_stake_full_limit",
            common + FunctionArgument("limit_price", optionalPrice),
            emptyList(),
            7 to 8
        )
        val claim = MetadataFunction(
            "claim_root_with_hotkey",
            listOf(FunctionArgument("hotkey", account)),
            emptyList(),
            7 to 10
        )
        val transfer = MetadataFunction(
            "transfer_keep_alive",
            listOf(FunctionArgument("dest", account), FunctionArgument("value", u64)),
            emptyList(),
            5 to 3
        )
        val modules = listOf(
            Module(
                "SubtensorModule",
                null,
                listOf(
                    root,
                    sale,
                    full,
                    claim
                ).associateBy { it.name },
                null,
                emptyMap(),
                emptyMap(),
                BigInteger.valueOf(7)
            ),
            Module(
                "Balances",
                null,
                mapOf(transfer.name to transfer),
                null,
                emptyMap(),
                emptyMap(),
                BigInteger.valueOf(5)
            )
        ).associateBy { it.name }
        val resolver = DynamicTypeResolver()
        val registry = TypeRegistry(mapOf("Address" to TypeReference(account)), resolver)
        val runtime = RuntimeSnapshot(
            registry,
            RuntimeMetadata(
                15,
                modules,
                ExtrinsicMetadata(BigInteger.valueOf(4), emptyList()),
                emptyList()
            )
        )
        val builder = mock(ExtrinsicBuilder::class.java)
        `when`(builder.runtime).thenReturn(runtime)
        return builder
    }
}
