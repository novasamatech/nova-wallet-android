package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorSubnetListTest {

    private val icon = Icon.FromDrawableRes(1)

    private val symbol = TokenSymbol("TEST")

    @Test
    fun `root and subnet sections belong to one ordered list`() {
        val row = row(5)
        assertEquals(
            listOf(BittensorSubnetGroup.ROOT, BittensorSubnetListItem.Root(icon, symbol), BittensorSubnetGroup.SUBNETS, BittensorSubnetListItem.Subnet(row)),
            bittensorSubnetList(icon, listOf(row), symbol)
        )
    }

    @Test
    fun `empty subnet search keeps root entry in the list`() {
        assertEquals(
            listOf(BittensorSubnetGroup.ROOT, BittensorSubnetListItem.Root(icon, symbol), BittensorSubnetGroup.SUBNETS),
            bittensorSubnetList(icon, emptyList(), symbol)
        )
    }

    @Test
    fun `grouping preserves the sorted subnet rows and wallet identity`() {
        val rows = listOf(row(9), row(1))
        assertEquals(rows, bittensorSubnetList(icon, rows, symbol).filterIsInstance<BittensorSubnetListItem.Subnet>().map { it.row })
    }

    @Test
    fun `root entry carries the supplied asset symbol`() {
        val root = bittensorSubnetList(icon, emptyList(), symbol).filterIsInstance<BittensorSubnetListItem.Root>().single()

        assertEquals(symbol, root.tokenSymbol)
    }

    private fun row(id: Int) = BittensorSubnetRowUi(
        subnet = BittensorSubnet(BittensorNetuid.Subnet(id), "Subnet $id", "alpha", null),
        metaId = 7L,
        favorite = false,
        price = "0.01 ${symbol.value}"
    )
}
