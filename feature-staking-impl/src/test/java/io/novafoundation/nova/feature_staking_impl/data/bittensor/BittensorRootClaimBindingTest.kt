package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class BittensorRootClaimBindingTest {
    private val unit = BigInteger.ONE.shiftLeft(32)

    @Test fun `unset threshold uses runtime default`() {
        assertEquals(BigInteger.valueOf(500_000), bittensorMinimumRootClaim(null))
    }

    @Test fun `fixed point threshold rounds fractional plank up`() {
        assertEquals(BigInteger.valueOf(3), bittensorMinimumRootClaim(unit * BigInteger.valueOf(2) + BigInteger.ONE))
        assertEquals(BigInteger.valueOf(2), bittensorMinimumRootClaim(unit * BigInteger.valueOf(2)))
        assertEquals(BigInteger.ZERO, bittensorMinimumRootClaim(BigInteger.ZERO))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative threshold is invalid`() {
        bittensorMinimumRootClaim(BigInteger.valueOf(-1))
    }

    @Test fun `decoded normalized preview preserves accrued and redeemable separately`() {
        val preview = bindBittensorRootClaimPreviews(listOf(preview(1, 1_000_000, 600_000, 400_000))).single()
        assertEquals(BigInteger.valueOf(1_000_000), preview.accrued)
        assertEquals(BigInteger.valueOf(600_000), preview.redeemable)
        assertEquals(BigInteger.valueOf(400_000), preview.forfeitedEstimate)
    }

    @Test fun `only redeemable rewards meeting threshold are claimable`() {
        val claims = BittensorRootClaims(
            bindBittensorRootClaimPreviews(
                listOf(
                    preview(1, 1_000_000, 499_999, 500_001),
                    preview(2, 500_000, 500_000, 0),
                    preview(3, 600_000, 600_000, 0)
                )
            ),
            BigInteger.valueOf(500_000)
        )
        assertEquals(listOf(BigInteger.valueOf(600_000), BigInteger.valueOf(500_000)), claims.claimable.map { it.redeemable })
    }

    @Test fun `zero remains unclaimable when threshold is zero`() {
        val claims = BittensorRootClaims(bindBittensorRootClaimPreviews(listOf(preview(1, 0, 0, 0))), BigInteger.ZERO)
        assertTrue(claims.claimable.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `duplicate hotkeys do not double rewards`() {
        bindBittensorRootClaimPreviews(listOf(preview(1, 1, 1, 0), preview(1, 1, 1, 0)))
    }

    @Test(expected = Exception::class)
    fun `missing redeemable is unavailable rather than zero`() {
        bindBittensorRootClaimPreviews(listOf(Struct.Instance(mapOf("hotkey" to ByteArray(32), "accruedTao" to BigInteger.ONE))))
    }

    @Test fun `stake availability uses decoded nested tuples`() {
        val coldkey = ByteArray(32) { 1 }
        val decoded = listOf(
            listOf(
                coldkey,
                listOf(
                    listOf(
                        BigInteger.valueOf(64),
                        Struct.Instance(
                            mapOf(
                                "total" to BigInteger.TEN,
                                "locked" to BigInteger.valueOf(3),
                                "available" to BigInteger.valueOf(7)
                            )
                        )
                    )
                )
            )
        )
        assertEquals(
            BittensorExitAvailability(BigInteger.TEN, BigInteger.valueOf(3), BigInteger.valueOf(7)),
            bindBittensorExitAvailability(decoded, coldkey, 64)
        )
    }

    @Test(expected = Exception::class)
    fun `availability cannot be read for another wallet`() {
        bindBittensorExitAvailability(listOf(listOf(ByteArray(32) { 2 }, emptyList<Any>())), ByteArray(32) { 1 }, 64)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `inconsistent availability is rejected`() {
        val coldkey = ByteArray(32)
        bindBittensorExitAvailability(
            listOf(
                listOf(
                    coldkey,
                    listOf(
                        listOf(
                            BigInteger.ONE,
                            Struct.Instance(
                                mapOf(
                                    "total" to BigInteger.TEN,
                                    "locked" to BigInteger.TEN,
                                    "available" to BigInteger.ONE
                                )
                            )
                        )
                    )
                )
            ),
            coldkey,
            1
        )
    }

    @Test fun `equal claim values use ascending hotkey order like iOS`() {
        val previews = bindBittensorRootClaimPreviews(
            listOf(preview(3, 600_000, 600_000, 0), preview(2, 600_000, 600_000, 0), preview(1, 500_000, 500_000, 0))
        )
        val claims = BittensorRootClaims(previews, BigInteger.valueOf(500_000))
        assertEquals(listOf<Byte>(2, 3, 1), claims.claimable.map { it.hotkey.value.first() })
    }

    private fun preview(key: Byte, accrued: Long, redeemable: Long, forfeited: Long) = Struct.Instance(
        mapOf(
            "hotkey" to ByteArray(32) { key },
            "accruedTao" to BigInteger.valueOf(accrued),
            "redeemableTao" to BigInteger.valueOf(redeemable),
            "forfeitedTaoEst" to BigInteger.valueOf(forfeited)
        )
    )
}
