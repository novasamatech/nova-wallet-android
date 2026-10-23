package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.ltrIsolated
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPosition
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitPositionPayload
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorPositionChoiceTest {

    private fun hotkey(byte: Int) = AccountIdKey(ByteArray(32) { byte.toByte() })

    private fun position(byte: Int, alpha: Long) = BittensorSubnetPosition(BittensorNetuid.Subnet(12), hotkey(byte), BigInteger.valueOf(alpha))

    private fun choices(held: List<BittensorSubnetPosition>, names: Map<AccountIdKey, String> = emptyMap()) = bittensorPositionChoices(
        held = held,
        metaId = 3L,
        names = names,
        addressOf = { "5Address${it.first()}" },
        formatStaked = { "$it μ" }
    )

    @Test
    fun `a named validator shows its name, otherwise the full address`() {
        val result = choices(listOf(position(1, 500), position(2, 10)), names = mapOf(hotkey(1) to "Taostats", hotkey(2) to " "))

        assertEquals(listOf("Taostats", "5Address2"), result.map { it.title })
    }

    @Test
    fun `each row carries the stake held with that validator`() {
        val result = choices(listOf(position(1, 500), position(2, 10)))

        assertEquals(listOf("500 μ".ltrIsolated(), "10 μ".ltrIsolated()), result.map { it.staked })
    }

    @Test
    fun `rows keep the held order and open the matching position`() {
        val result = choices(listOf(position(4, 7), position(9, 3)))

        assertEquals(
            listOf(BittensorExitPositionPayload(3L, 12, "5Address4"), BittensorExitPositionPayload(3L, 12, "5Address9")),
            result.map { it.position }
        )
        assertEquals(listOf(hotkey(4), hotkey(9)), result.map { it.hotkey })
    }
}
