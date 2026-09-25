package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorSubnetListTest {

    private val icon = Icon.FromDrawableRes(1)

    @Test
    fun `root and subnet sections belong to one ordered list`() {
        val row = row(5)
        assertEquals(
            listOf(BittensorSubnetGroup.ROOT, BittensorSubnetListItem.Root(icon), BittensorSubnetGroup.SUBNETS, BittensorSubnetListItem.Subnet(row)),
            bittensorSubnetList(icon, listOf(row))
        )
    }

    @Test
    fun `empty subnet search keeps root entry in the list`() {
        assertEquals(
            listOf(BittensorSubnetGroup.ROOT, BittensorSubnetListItem.Root(icon), BittensorSubnetGroup.SUBNETS),
            bittensorSubnetList(icon, emptyList())
        )
    }

    @Test
    fun `grouping preserves the sorted subnet rows and wallet identity`() {
        val rows = listOf(row(9), row(1))
        assertEquals(rows, bittensorSubnetList(icon, rows).filterIsInstance<BittensorSubnetListItem.Subnet>().map { it.row })
    }

    private fun row(id: Int) = BittensorSubnetRowUi(
        subnet = BittensorSubnet(BittensorSubnetId(id), "Subnet $id", "alpha", null),
        metaId = 7L,
        favorite = false,
        price = "0.01 TAO"
    )
}
