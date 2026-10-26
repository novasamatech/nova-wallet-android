package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationKind
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOutcomeTarget
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.actualSellReceive
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.actualRootStakeMoved
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import io.novasama.substrate_sdk_android.runtime.metadata.module.Event
import io.novasama.substrate_sdk_android.runtime.metadata.module.Module
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class BittensorOperationOutcomeBindingTest {

    private val cold = AccountIdKey(ByteArray(32) { 1 })
    private val hot = AccountIdKey(ByteArray(32) { 2 })
    private val otherHot = AccountIdKey(ByteArray(32) { 3 })
    private val beneficiary = AccountIdKey(ByteArray(32) { 4 })
    private val netuid = BittensorNetuid.Subnet(7)
    private val target = BittensorOutcomeTarget(BittensorOperationKind.STAKE, netuid, setOf(hot), beneficiary)

    @Test
    fun `matches exact coldkey hotkey and subnet in integer units`() {
        val huge = BigInteger.ONE.shiftLeft(60)
        val events = listOf(
            change(hot, tao = huge),
            change(otherHot),
            change(hot, account = beneficiary),
            change(hot, subnet = 8)
        )
        val result = parse(events)
        assertEquals(huge, result.tao)
        assertEquals(BigInteger.TEN, result.alpha)
    }

    @Test
    fun `group requires every reviewed hotkey and sums repeated target changes`() {
        val group = target.copy(hotkeys = setOf(hot, otherHot))
        assertNull(parse(listOf(change(hot)), group).tao)
        assertEquals(BigInteger.valueOf(60), parse(listOf(change(hot), change(hot), change(otherHot)), group).tao)
        assertEquals(BigInteger.valueOf(40), parse(listOf(change(hot), change(otherHot)), group).tao)
    }

    @Test
    fun `missing and malformed events remain unknown not zero`() {
        assertNull(parse(emptyList()).tao)
        assertNull(parse(emptyList()).networkFee)
        assertNull(parse(emptyList()).serviceFee)
        val malformed = event("SubtensorModule", "StakeAdded", emptyList())
        assertNull(parse(listOf(malformed, change(hot))).tao)
    }

    @Test
    fun `service fee needs exact sender and beneficiary`() {
        val good = event("Balances", "Transfer", listOf(cold.value, beneficiary.value, BigInteger.TEN))
        val foreign = event("Balances", "Transfer", listOf(otherHot.value, beneficiary.value, BigInteger.ONE))
        assertEquals(BigInteger.TEN, parse(listOf(good, foreign)).serviceFee)
        assertNull(parse(listOf(foreign)).serviceFee)
        assertEquals(BigInteger.valueOf(20), parse(listOf(good, good)).serviceFee)
    }

    @Test
    fun `known payer uses reported actual fee without adding nonzero tip`() {
        val fee = event("TransactionPayment", "TransactionFeePaid", listOf(cold.value, BigInteger.TEN, BigInteger.ZERO))
        val tip = event("TransactionPayment", "TransactionFeePaid", listOf(cold.value, BigInteger.TEN, BigInteger.ONE))
        assertEquals(BigInteger.TEN, parse(listOf(fee)).networkFee)
        assertEquals(BigInteger.TEN, parse(listOf(tip)).networkFee)
        assertNull(bindBittensorOperationOutcome(listOf(fee), target, cold, beneficiary, "block").networkFee)
        assertNull(bindBittensorOperationOutcome(listOf(fee), target, cold, null, "block").networkFee)
    }

    @Test
    fun `alpha fee declaration position does not change first withdrawal exclusion`() {
        val sell = target.copy(kind = BittensorOperationKind.UNSTAKE)
        val removed = change(hot, name = "StakeRemoved")
        val feeSale = change(hot, tao = BigInteger.ONE, name = "StakeRemoved")
        val fee = event("SubtensorModule", "TransactionFeePaidWithAlpha", listOf(cold.value, BigInteger.valueOf(7), BigInteger.ONE, BigInteger.TEN))
        listOf(listOf(fee, feeSale, removed), listOf(feeSale, removed, fee), listOf(feeSale, fee, removed)).forEach { events ->
            val result = parse(events, sell)
            assertEquals(BigInteger.valueOf(20), result.tao)
            assertEquals(BigInteger.valueOf(11), result.alpha)
            assertEquals(BigInteger.TEN, result.networkFee)
            assertEquals(BigInteger.ONE, result.alphaFee)
        }
    }

    @Test
    fun `sell alpha includes event fee without inventing net receive`() {
        val sell = target.copy(kind = BittensorOperationKind.UNSTAKE)
        val result = parse(listOf(change(hot, name = "StakeRemoved")), sell)
        assertEquals(BigInteger.valueOf(11), result.alpha)
        assertEquals(BigInteger.valueOf(20), result.tao)
        assertNull(result.serviceFee)
    }

    @Test
    fun `claim prefers basket and falls back to unique root event for single target`() {
        val claim = target.copy(kind = BittensorOperationKind.CLAIM, netuid = BittensorNetuid.Root)
        val basket = event("SubtensorModule", "BasketClaimed", listOf(hot.value, cold.value, BigInteger.TEN))
        val root = event("SubtensorModule", "RootClaimed", listOf(cold.value, BigInteger.valueOf(100)))
        assertEquals(BigInteger.TEN, parse(listOf(basket, root), claim).tao)
        assertEquals(BigInteger.valueOf(100), parse(listOf(root), claim).tao)
        assertNull(parse(listOf(root, root), claim).tao)
        assertNull(parse(listOf(root), claim.copy(hotkeys = setOf(hot, otherHot))).tao)
        assertNull(parse(listOf(root), claim.copy(netuid = netuid)).tao)
    }

    @Test
    fun `root claim fallback rejects conflicting basket but ignores another accounts basket`() {
        val claim = target.copy(kind = BittensorOperationKind.CLAIM, netuid = BittensorNetuid.Root)
        val root = event("SubtensorModule", "RootClaimed", listOf(cold.value, BigInteger.TEN))
        val conflict = event("SubtensorModule", "BasketClaimed", listOf(otherHot.value, cold.value, BigInteger.ONE))
        val foreign = event("SubtensorModule", "BasketClaimed", listOf(otherHot.value, beneficiary.value, BigInteger.ONE))
        assertNull(parse(listOf(root, conflict), claim).tao)
        assertEquals(BigInteger.TEN, parse(listOf(root, foreign), claim).tao)
        assertNull(parse(listOf(event("SubtensorModule", "RootClaimed", listOf(beneficiary.value, BigInteger.TEN))), claim).tao)
    }

    @Test
    fun `fee sale matches coldkey and fee netuid not target hotkey and excludes only first`() {
        val sell = target.copy(kind = BittensorOperationKind.UNSTAKE)
        val fee = alphaFee()
        val foreignAccount = change(hot, account = beneficiary, name = "StakeRemoved")
        val foreignSubnet = change(hot, subnet = 8, name = "StakeRemoved")
        val feeSale = change(otherHot, name = "StakeRemoved")
        val first = change(hot, tao = BigInteger.valueOf(100), name = "StakeRemoved")
        val second = change(hot, tao = BigInteger.valueOf(50), name = "StakeRemoved")
        val result = parse(listOf(foreignAccount, foreignSubnet, feeSale, first, second, fee), sell)
        assertEquals(BigInteger.valueOf(150), result.tao)
        assertEquals(BigInteger.valueOf(22), result.alpha)
        assertEquals(BigInteger.TEN, result.networkFee)
        assertEquals(BigInteger.valueOf(150), result.actualSellReceive(BigInteger.ZERO))
    }

    @Test
    fun `alpha native equivalent takes precedence over native fee and is not deducted twice`() {
        val sell = target.copy(kind = BittensorOperationKind.UNSTAKE)
        val feeSale = change(hot, name = "StakeRemoved")
        val trade = change(hot, tao = BigInteger.valueOf(100), name = "StakeRemoved")
        val service = event("Balances", "Transfer", listOf(cold.value, beneficiary.value, BigInteger.ONE))
        val native = event("TransactionPayment", "TransactionFeePaid", listOf(cold.value, BigInteger.valueOf(999), BigInteger.TEN))
        val result = parse(listOf(feeSale, trade, service, alphaFee(), native), sell)
        assertEquals(BigInteger.TEN, result.networkFee)
        assertEquals(BigInteger.valueOf(99), result.actualSellReceive(BigInteger.ONE))
    }

    @Test
    fun `alpha fee requires payer valid fee subnet and complete shape`() {
        val sell = target.copy(kind = BittensorOperationKind.UNSTAKE)
        val removed = change(hot, name = "StakeRemoved")
        val invalidFees = listOf(
            alphaFee(payer = beneficiary),
            alphaFee(subnet = -1),
            alphaFee(subnet = UShort.MAX_VALUE.toInt() + 1),
            event("SubtensorModule", "TransactionFeePaidWithAlpha", listOf(cold.value, BigInteger.valueOf(7), BigInteger.ONE)),
            alphaFee(tao = BigInteger.valueOf(-1))
        )
        invalidFees.forEach { fee ->
            val result = parse(listOf(removed, fee), sell)
            assertNull(result.alphaFee)
            assertNull(result.networkFee)
            assertNull(result.tao)
        }
    }

    @Test
    fun `other subnet fee retains identity and does not exclude target trade or deduct native fee twice`() {
        val sell = target.copy(kind = BittensorOperationKind.UNSTAKE)
        val trade = change(hot, tao = BigInteger.valueOf(100), name = "StakeRemoved")
        val feeSale = change(otherHot, subnet = 8, tao = BigInteger.TEN, name = "StakeRemoved")
        val fee = alphaFee(subnet = 8)
        // The target trade precedes the fee-subnet sale: exclusion must use the fee's subnet.
        val result = parse(listOf(trade, feeSale, fee), sell)
        assertEquals(BigInteger.valueOf(100), result.tao)
        assertEquals(BigInteger.valueOf(11), result.alpha)
        assertEquals(BigInteger.TEN, result.networkFee)
        assertEquals(BigInteger.ONE, result.alphaFee)
        assertEquals(BittensorNetuid.Subnet(8), result.alphaFeeNetuid)
        assertEquals(BigInteger.valueOf(100), result.actualSellReceive(BigInteger.ZERO))
    }

    @Test
    fun `root withdrawal paid with different subnet alpha never adds that alpha to native stake`() {
        val root = target.copy(kind = BittensorOperationKind.UNSTAKE, netuid = BittensorNetuid.Root)
        val trade = change(hot, subnet = 0, tao = BigInteger.valueOf(100), name = "StakeRemoved")
        val feeSale = change(otherHot, subnet = 8, name = "StakeRemoved")
        val result = parse(listOf(feeSale, trade, alphaFee(subnet = 8)), root)
        assertEquals(BigInteger.valueOf(100), result.actualRootStakeMoved())
        assertEquals(BigInteger.TEN, result.networkFee)
        assertEquals(BittensorNetuid.Subnet(8), result.alphaFeeNetuid)
    }

    @Test
    fun `delegated alpha payer does not remove executing wallets trade`() {
        val sell = target.copy(kind = BittensorOperationKind.UNSTAKE)
        val events = listOf(change(hot, name = "StakeRemoved"), alphaFee(payer = beneficiary))
        val result = bindBittensorOperationOutcome(events, sell, cold, beneficiary, "block")
        assertEquals(BigInteger.valueOf(20), result.tao)
        assertEquals(BigInteger.TEN, result.networkFee)
        assertEquals(BigInteger.valueOf(20), result.actualSellReceive(BigInteger.ZERO))
    }

    @Test
    fun `ios sell fixture sums remaining changes and reports fee native equivalent`() {
        val sell = target.copy(kind = BittensorOperationKind.UNSTAKE)
        fun removed(tao: Long, alpha: Long, poolFee: Long) = event(
            "SubtensorModule",
            "StakeRemoved",
            listOf(cold.value, hot.value, BigInteger.valueOf(tao), BigInteger.valueOf(alpha), BigInteger.valueOf(7), BigInteger.valueOf(poolFee))
        )
        val feeSale = removed(734_304, 95_364_155, 0)
        val trade = removed(3_840_000_000, 499_750_000_000, 250_000_000)
        val additional = removed(19_500_000, 2_598_700_000, 1_300_000)
        val alphaFee = event(
            "SubtensorModule",
            "TransactionFeePaidWithAlpha",
            listOf(cold.value, BigInteger.valueOf(7), BigInteger.valueOf(95_364_155), BigInteger.valueOf(734_304))
        )
        val nativeFee = event("TransactionPayment", "TransactionFeePaid", listOf(cold.value, BigInteger.valueOf(700_000), BigInteger.ZERO))
        val service = event("Balances", "Transfer", listOf(cold.value, beneficiary.value, BigInteger.valueOf(11_467_258)))
        val result = parse(listOf(feeSale, trade, additional, service, alphaFee, nativeFee), sell)
        assertEquals(BigInteger.valueOf(3_859_500_000), result.tao)
        assertEquals(BigInteger.valueOf(502_600_000_000), result.alpha)
        assertEquals(BigInteger.valueOf(95_364_155), result.alphaFee)
        assertEquals(BigInteger.valueOf(734_304), result.networkFee)
        assertEquals(BigInteger.valueOf(3_848_032_742), result.actualSellReceive(BigInteger.valueOf(11_467_258)))
    }

    @Test
    fun `exclusion follows first matching event not guessed fee amount`() {
        val sell = target.copy(kind = BittensorOperationKind.UNSTAKE)
        val first = change(hot, tao = BigInteger.valueOf(100), name = "StakeRemoved")
        val second = change(hot, tao = BigInteger.ONE, name = "StakeRemoved")
        assertEquals(BigInteger.ONE, parse(listOf(first, second, alphaFee()), sell).tao)
        assertEquals(BigInteger.valueOf(100), parse(listOf(second, first, alphaFee()), sell).tao)
        assertNull(parse(listOf(first, alphaFee()), sell).tao)
    }

    @Test
    fun `zero root claim is confirmed zero while malformed aggregate remains unavailable`() {
        val claim = target.copy(kind = BittensorOperationKind.CLAIM, netuid = BittensorNetuid.Root)
        val zero = event("SubtensorModule", "RootClaimed", listOf(cold.value, BigInteger.ZERO))
        val malformed = event("SubtensorModule", "RootClaimed", listOf(cold.value))
        assertEquals(BigInteger.ZERO, parse(listOf(zero), claim).tao)
        assertNull(parse(listOf(malformed), claim).tao)
    }

    private fun alphaFee(payer: AccountIdKey = cold, subnet: Int = 7, tao: BigInteger = BigInteger.TEN) =
        event("SubtensorModule", "TransactionFeePaidWithAlpha", listOf(payer.value, BigInteger.valueOf(subnet.toLong()), BigInteger.ONE, tao))

    private fun parse(events: List<GenericEvent.Instance>, context: BittensorOutcomeTarget = target) =
        bindBittensorOperationOutcome(events, context, cold, cold, "block")

    private fun change(
        hotkey: AccountIdKey,
        account: AccountIdKey = cold,
        subnet: Int = 7,
        tao: BigInteger = BigInteger.valueOf(20),
        name: String = "StakeAdded"
    ) = event("SubtensorModule", name, listOf(account.value, hotkey.value, tao, BigInteger.TEN, BigInteger.valueOf(subnet.toLong()), BigInteger.ONE))

    private fun event(moduleName: String, name: String, args: List<Any?>): GenericEvent.Instance {
        val module = mock(Module::class.java)
        `when`(module.name).thenReturn(moduleName)
        val type = Event(name, index = 0 to 0, documentation = emptyList(), arguments = emptyList())
        return GenericEvent.Instance(module, type, args)
    }
}
