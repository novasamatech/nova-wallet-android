package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorExitDustLegTest {

    private val price = BigInteger("24121195")
    private val feeRate = BittensorWithdrawalBounds.DEFAULT_SWAP_FEE_RATE
    private val floors = BittensorExitFloors(
        bittensorWithdrawalFloor(BigInteger("2000000"), price, feeRate),
        bittensorWithdrawalFloor(BigInteger("2000000"), price, BigInteger.ZERO),
        bittensorWithdrawalFloor(BigInteger.ONE, price, feeRate)
    )

    private fun position(id: Int, amount: Long, locked: Long = 0) = BittensorExitBasisPosition(
        AccountIdKey(ByteArray(32) { id.toByte() }), BigInteger.valueOf(amount), BigInteger.valueOf(locked), false, false
    )

    private fun group(positions: List<BittensorExitBasisPosition>, bounds: BittensorExitFloors = floors) =
        BittensorExitGroup(positions, positions.sumOf { it.alpha }, bounds)

    @Test
    fun `SN9 all skips one rao dust without consuming a batch leg`() {
        val real = position(232, 1437781835)
        val dust = position(164, 1)
        val group = group(listOf(real, dust))
        assertEquals(BigInteger.valueOf(43), floors.exit)
        assertEquals(setOf(dust.hotkey), group.unsellableHotkeys)
        assertEquals(real.alpha, group.maximumFill(BittensorExitSelection.All))
        val plan = group.basis(BittensorExitSelection.All).plan(real.alpha, floors)!!
        assertEquals(listOf(BittensorExitSlice(real.hotkey, real.alpha, true)), plan.slices)
        assertEquals(BigInteger.ZERO, group.maximum(BittensorExitSelection.Validator(dust.hotkey)))
    }

    @Test
    fun `dust does not affect planning for any sampled partial amount`() {
        val real = position(1, 1437781835)
        val dust = position(2, 1)
        val clean = group(listOf(real)).basis(BittensorExitSelection.All)
        val dusty = group(listOf(real, dust)).basis(BittensorExitSelection.All)
        for (part in 1..2000) {
            val amount = real.alpha * BigInteger.valueOf(part.toLong()) / BigInteger.valueOf(2000)
            assertEquals(clean.plan(amount, floors), dusty.plan(amount, floors))
        }
    }

    @Test
    fun `root and locked positions retain existing rules and exit boundary is inclusive`() {
        val rootFloors = floors.copy(exit = BigInteger.ZERO)
        assertEquals(BigInteger.ONE, position(1, 1).drawable(rootFloors))
        assertEquals(floors.exit, position(1, floors.exit.toLong()).drawable(floors))
        assertEquals(BigInteger.ZERO, position(1, floors.exit.toLong() - 1).drawable(floors))
        val locked = position(2, 100, 1)
        assertEquals(locked.drawable(rootFloors), locked.drawable(floors))
    }
}
