package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorPositionBindingTest {

    @Test
    fun `root positions exclude subnet stakes and empty positions`() {
        val positions = bindBittensorRootPositions(listOf(position(0, 10), position(1, 100), position(0, 0)))

        assertEquals(1, positions.size)
        assertEquals(
            BigInteger.TEN,
            positions.single().stakeInPlanks
        )
    }

    @Test
    fun `root positions preserve multiple validators`() {
        val positions = bindBittensorRootPositions(listOf(position(0, 10, 1), position(0, 20, 2)))

        assertEquals(2, positions.size)
        assertTrue(positions[0].validatorHotkey != positions[1].validatorHotkey)
        assertEquals(
            BigInteger.valueOf(30),
            positions.sumOf { it.stakeInPlanks }
        )
    }

    @Test
    fun `empty response means no stake`() {
        assertTrue(
            bindBittensorRootPositions(emptyList<Any>())
                .isEmpty()
        )
    }

    @Test(expected = Exception::class)
    fun `malformed response is not converted into an empty position`() {
        bindBittensorRootPositions(listOf(Struct.Instance(mapOf("netuid" to BigInteger.ZERO))))
    }

    @Test
    fun `registration uses the normalized SDK field and remains unknown when absent`() {
        val raw = position(64, 100)
        val known = Struct.Instance(raw.mapping + ("isRegistered" to true))
        assertEquals(
            true,
            bindBittensorSubnetPositions(listOf(known))
                .single().isRegistered
        )
        assertEquals(
            null,
            bindBittensorSubnetPositions(listOf(raw))
                .single().isRegistered
        )
    }

    private fun position(netuid: Long, stake: Long, hotkey: Byte = 1) = Struct.Instance(
        mapOf(
            "netuid" to BigInteger.valueOf(netuid),
            "stake" to BigInteger.valueOf(stake),
            "hotkey" to ByteArray(32) { hotkey }
        )
    )

    @Test
    fun `subnet positions exclude root and keep validators separate`() {
        val positions = bindBittensorSubnetPositions(
            listOf(
                position(0, 10),
                position(64, 100, 1),
                position(64, 200, 2),
                position(1, 0)
            )
        )

        assertEquals(2, positions.size)
        assertTrue(positions.all { it.subnetId.value == 64 })
        assertTrue(positions[0].validatorHotkey != positions[1].validatorHotkey)
        assertEquals(
            BigInteger.valueOf(300),
            positions.sumOf { it.alphaInPlanks }
        )
    }

    @Test
    fun `subnet position validates id and atomic balance`() {
        assertTrue(
            runCatching { bindBittensorSubnetPositions(listOf(position(65536, 1))) }
                .isFailure
        )
        assertTrue(
            runCatching { bindBittensorSubnetPositions(listOf(position(64, -1))) }
                .isFailure
        )
    }
}
