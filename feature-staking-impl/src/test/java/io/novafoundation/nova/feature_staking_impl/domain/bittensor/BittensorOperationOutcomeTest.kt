package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BittensorOperationOutcomeTest {

    private val coldkey = AccountIdKey(ByteArray(32) { 1 })

    private val otherPayer = AccountIdKey(ByteArray(32) { 2 })

    private val outcome = BittensorOperationOutcome(
        blockHash = "block",
        tao = BigInteger.valueOf(100),
        alpha = BigInteger.valueOf(200),
        serviceFee = BigInteger.TEN,
        networkFee = BigInteger.ONE,
        feePayer = coldkey,
        alphaFee = null,
        netuid = BittensorNetuid.Subnet(7),
        coldkey = coldkey
    )

    @Test
    fun `buy total uses executed trade and service transfer but excludes separate network fee`() {
        assertEquals(BigInteger.valueOf(110), outcome.actualBuyPay(BigInteger.TEN))
        assertNull(outcome.copy(serviceFee = null).actualBuyPay(BigInteger.TEN))
        assertNull(outcome.copy(tao = null).actualBuyPay(BigInteger.ZERO))
    }

    @Test
    fun `absent planned service call permits zero but absent emitted transfer does not`() {
        val withoutTransfer = outcome.copy(serviceFee = null)
        assertEquals(BigInteger.valueOf(100), withoutTransfer.actualBuyPay(BigInteger.ZERO))
        assertEquals(BigInteger.valueOf(99), withoutTransfer.actualSellReceive(BigInteger.ZERO))
        assertNull(withoutTransfer.actualSellReceive(BigInteger.TEN))
    }

    @Test
    fun `sell receive deducts network fee only from its proven payer`() {
        assertEquals(BigInteger.valueOf(89), outcome.actualSellReceive(BigInteger.TEN))
        val delegated = outcome.copy(feePayer = otherPayer, networkFee = null)
        assertEquals(BigInteger.valueOf(90), delegated.actualSellReceive(BigInteger.TEN))
        assertNull(outcome.copy(feePayer = null).actualSellReceive(BigInteger.TEN))
    }

    @Test
    fun `missing native fee or trade remains unknown but alpha paid fee is not deducted twice`() {
        assertNull(outcome.copy(networkFee = null).actualSellReceive(BigInteger.TEN))
        assertEquals(BigInteger.valueOf(90), outcome.copy(alphaFee = BigInteger.ONE).actualSellReceive(BigInteger.TEN))
        assertNull(outcome.copy(tao = null).actualSellReceive(BigInteger.TEN))
    }

    @Test
    fun `negative receive is unknown not clamped to a fictitious zero`() {
        assertNull(outcome.copy(tao = BigInteger.ONE).actualSellReceive(BigInteger.TEN))
        assertEquals(BigInteger.ZERO, outcome.copy(tao = BigInteger.valueOf(11)).actualSellReceive(BigInteger.TEN))
    }

    @Test
    fun `root stake moved includes own fee sale but never delegates fee or subnet alpha`() {
        val root = outcome.copy(netuid = BittensorNetuid.Root, alphaFee = BigInteger.TEN, alphaFeeNetuid = BittensorNetuid.Root)
        assertEquals(BigInteger.valueOf(110), root.actualRootStakeMoved())
        assertEquals(BigInteger.valueOf(100), root.copy(feePayer = otherPayer).actualRootStakeMoved())
        assertEquals(BigInteger.valueOf(100), root.copy(alphaFee = null).actualRootStakeMoved())
        assertEquals(BigInteger.valueOf(100), root.copy(alphaFeeNetuid = BittensorNetuid.Subnet(8)).actualRootStakeMoved())
        assertNull(root.copy(alphaFeeNetuid = null).actualRootStakeMoved())
        assertNull(root.copy(tao = null).actualRootStakeMoved())
        assertNull(outcome.actualRootStakeMoved())
    }

    @Test
    fun `integer totals retain values beyond double precision`() {
        val huge = BigInteger.ONE.shiftLeft(100)
        assertEquals(huge + BigInteger.TEN, outcome.copy(tao = huge).actualBuyPay(BigInteger.TEN))
        assertEquals(huge - BigInteger.valueOf(11), outcome.copy(tao = huge).actualSellReceive(BigInteger.TEN))
    }
}
