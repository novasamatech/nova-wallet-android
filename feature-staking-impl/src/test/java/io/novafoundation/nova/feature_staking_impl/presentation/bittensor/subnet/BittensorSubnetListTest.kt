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
    fun `root and subnet rows belong to one list without group headers`() {
        val row = row(5)
        assertEquals(
            listOf(BittensorSubnetListItem.Root(icon), BittensorSubnetListItem.Subnet(row)),
            bittensorSubnetList(icon, listOf(row))
        )
    }

    @Test
    fun `empty subnet search keeps root entry in the list`() {
        assertEquals(
            listOf(BittensorSubnetListItem.Root(icon)),
            bittensorSubnetList(icon, emptyList())
        )
    }

    @Test
    fun `grouping preserves the sorted subnet rows and wallet identity`() {
        val rows = listOf(row(9), row(1))
        assertEquals(rows, bittensorSubnetList(icon, rows).filterIsInstance<BittensorSubnetListItem.Subnet>().map { it.row })
    }

    @Test
    fun `root entry is available before native asset or subnet metadata`() {
        val root = bittensorSubnetList(icon, emptyList()).filterIsInstance<BittensorSubnetListItem.Root>().single()

        assertEquals(icon, root.icon)
    }

    private fun row(id: Int) = BittensorSubnetRowUi(
        subnet = BittensorSubnet(BittensorNetuid.Subnet(id), "Subnet $id", "alpha", null),
        metaId = 7L,
        favorite = false,
        price = "0.01 ${symbol.value}"
    )
}
