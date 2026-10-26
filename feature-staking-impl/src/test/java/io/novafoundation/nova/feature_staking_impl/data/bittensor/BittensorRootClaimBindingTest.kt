package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumberOrZero
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorRootClaimBindingTest {

    private val unit = BigInteger.ONE.shiftLeft(32)

    @Test
    fun `unset root hold interval and last stake block mean no hold`() {
        assertEquals(BigInteger.ZERO, bindNumberOrZero(null))
        assertEquals(BigInteger.valueOf(42), bindNumberOrZero(BigInteger.valueOf(42)))
    }

    @Test(expected = IllegalStateException::class)
    fun `malformed nonempty root hold does not silently become zero`() {
        bindNumberOrZero("42")
    }

    @Test
    fun `distributed rewards cannot be combined to reach the minimum`() {
        val claims = claimsWithRewards(200_000, 300_000)

        assertTrue(claims.claimable.isEmpty())
        assertTrue(claims.hasRewardsBelowMinimum)
        assertEquals(BigInteger.valueOf(500_000), claims.totalRedeemable)
    }

    @Test
    fun `exact minimum is claimable without a minimum hint`() {
        val claims = claimsWithRewards(500_000)

        assertEquals(1, claims.claimable.size)
        assertFalse(claims.hasRewardsBelowMinimum)
    }

    @Test
    fun `eligible validator allows claiming despite another below minimum`() {
        val claims = claimsWithRewards(200_000, 600_000)

        assertEquals(listOf(BigInteger.valueOf(600_000)), claims.claimable.map { it.redeemable })
        assertFalse(claims.hasRewardsBelowMinimum)
    }

    @Test
    fun `no rewards do not require a minimum hint`() {
        assertFalse(claimsWithRewards().hasRewardsBelowMinimum)
        assertFalse(claimsWithRewards(0).hasRewardsBelowMinimum)
    }

    private fun claimsWithRewards(vararg amounts: Long): BittensorRootClaims {
        val previews = amounts.mapIndexed { index, amount ->
            preview((index + 1).toByte(), amount, amount, 0)
        }
        return BittensorRootClaims(
            previews = bindBittensorRootClaimPreviews(previews),
            minimum = BigInteger.valueOf(500_000)
        )
    }

    @Test
    fun `accrued but entirely forfeited rewards do not show a minimum hint`() {
        val previews = bindBittensorRootClaimPreviews(listOf(preview(1, 100_000, 0, 100_000)))
        val claims = BittensorRootClaims(previews, BigInteger.valueOf(500_000))

        assertFalse(claims.hasRewardsBelowMinimum)
        assertTrue(claims.claimable.isEmpty())
        assertEquals(BigInteger.ZERO, claims.totalRedeemable)
    }

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
        val preview = bindBittensorRootClaimPreviews(
            listOf(
                preview(
                    1,
                    1_000_000,
                    600_000,
                    400_000
                )
            )
        )
            .single()
        assertEquals(BigInteger.valueOf(1_000_000), preview.accrued)
        assertEquals(BigInteger.valueOf(600_000), preview.redeemable)
        assertEquals(BigInteger.valueOf(400_000), preview.forfeitedEstimate)
    }

    @Test fun `only redeemable rewards meeting threshold are claimable`() {
        val claims = BittensorRootClaims(
            bindBittensorRootClaimPreviews(
                listOf(
                    preview(
                        1,
                        1_000_000,
                        499_999,
                        500_001
                    ),
                    preview(
                        2,
                        500_000,
                        500_000,
                        0
                    ),
                    preview(
                        3,
                        600_000,
                        600_000,
                        0
                    )
                )
            ),
            BigInteger.valueOf(500_000)
        )
        assertEquals(
            listOf(BigInteger.valueOf(600_000), BigInteger.valueOf(500_000)),
            claims.claimable.map { it.redeemable }
        )
    }

    @Test fun `zero remains unclaimable when threshold is zero`() {
        val claims = BittensorRootClaims(
            bindBittensorRootClaimPreviews(
                listOf(
                    preview(
                        1,
                        0,
                        0,
                        0
                    )
                )
            ),
            BigInteger.ZERO
        )
        assertTrue(claims.claimable.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `duplicate hotkeys do not double rewards`() {
        bindBittensorRootClaimPreviews(
            listOf(
                preview(
                    1,
                    1,
                    1,
                    0
                ),
                preview(
                    1,
                    1,
                    1,
                    0
                )
            )
        )
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
        bindBittensorExitAvailability(
            listOf(
                listOf(
                    ByteArray(32) { 2 },
                    emptyList<Any>()
                )
            ),
            ByteArray(32) { 1 },
            64
        )
    }

    @Test fun `availability net of miner collateral is read as reported`() {
        val coldkey = ByteArray(32)
        assertEquals(
            BittensorExitAvailability(BigInteger.TEN, BigInteger.valueOf(3), BigInteger.valueOf(5)),
            bindBittensorExitAvailability(availabilityResponse(coldkey, netuid = 1, total = 10, locked = 3, available = 5), coldkey, 1)
        )
    }

    @Test fun `lock above the stake leaves nothing available`() {
        val coldkey = ByteArray(32)
        assertEquals(
            BittensorExitAvailability(BigInteger.valueOf(4), BigInteger.valueOf(6), BigInteger.ZERO),
            bindBittensorExitAvailability(availabilityResponse(coldkey, netuid = 1, total = 4, locked = 6, available = 0), coldkey, 1)
        )
    }

    @Test fun `subnet omitted by the runtime has nothing available`() {
        val coldkey = ByteArray(32)
        assertEquals(
            BittensorExitAvailability(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO),
            bindBittensorExitAvailability(availabilityResponse(coldkey, netuid = 1, total = 10, locked = 0, available = 10), coldkey, 64)
        )
    }

    @Test fun `equal claim values use ascending hotkey order like iOS`() {
        val previews = bindBittensorRootClaimPreviews(
            listOf(
                preview(
                    3,
                    600_000,
                    600_000,
                    0
                ),
                preview(
                    2,
                    600_000,
                    600_000,
                    0
                ),
                preview(
                    1,
                    500_000,
                    500_000,
                    0
                )
            )
        )
        val claims = BittensorRootClaims(previews, BigInteger.valueOf(500_000))
        assertEquals(
            listOf<Byte>(2, 3, 1),
            claims.claimable.map { it.hotkey.value.first() }
        )
    }

    private fun availabilityResponse(
        coldkey: ByteArray,
        netuid: Long,
        total: Long,
        locked: Long,
        available: Long
    ) = listOf(
        listOf(
            coldkey,
            listOf(
                listOf(
                    BigInteger.valueOf(netuid),
                    Struct.Instance(
                        mapOf(
                            "total" to BigInteger.valueOf(total),
                            "locked" to BigInteger.valueOf(locked),
                            "available" to BigInteger.valueOf(available)
                        )
                    )
                )
            )
        )
    )

    private fun preview(
        key: Byte,
        accrued: Long,
        redeemable: Long,
        forfeited: Long
    ) = Struct.Instance(
        mapOf(
            "hotkey" to ByteArray(32) { key },
            "accruedTao" to BigInteger.valueOf(accrued),
            "redeemableTao" to BigInteger.valueOf(redeemable),
            "forfeitedTaoEst" to BigInteger.valueOf(forfeited)
        )
    )
}
