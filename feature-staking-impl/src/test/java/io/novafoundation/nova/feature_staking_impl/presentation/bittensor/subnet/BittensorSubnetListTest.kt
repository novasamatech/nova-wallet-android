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
    fun `empty filter result keeps root entry in the list`() {
        assertEquals(
            listOf(BittensorSubnetListItem.Root(icon)),
            bittensorSubnetList(icon, emptyList())
        )
    }

    @Test
    fun `search without matches hides the root entry`() {
        assertEquals(emptyList<BittensorSubnetListItem>(), bittensorSubnetList(icon, emptyList(), searchHasNoMatches = true))
    }

    @Test
    fun `a search with no match in the unfiltered catalogue is a no-match state`() {
        assertEquals(BittensorSubnetEmptyKind.NO_SEARCH_MATCHES, emptyKind(query = "xyz", queryMatches = 0, shown = 0))
    }

    @Test
    fun `search matches hidden by filters keep the controls and show the filters-empty state`() {
        assertEquals(BittensorSubnetEmptyKind.FILTERED_OUT, emptyKind(query = "chutes", queryMatches = 2, shown = 0))
    }

    @Test
    fun `filters hiding every subnet without a search show the filters-empty state`() {
        assertEquals(BittensorSubnetEmptyKind.FILTERED_OUT, emptyKind(query = "", queryMatches = 64, shown = 0))
    }

    @Test
    fun `an empty catalogue without a search shows no subnets`() {
        assertEquals(BittensorSubnetEmptyKind.NO_SUBNETS, emptyKind(query = " ", catalogueSize = 0, queryMatches = 0, shown = 0))
    }

    @Test
    fun `shown rows, loading and failure are never empty states`() {
        assertEquals(BittensorSubnetEmptyKind.NONE, emptyKind(query = "xyz", queryMatches = 1, shown = 1))
        assertEquals(BittensorSubnetEmptyKind.NONE, emptyKind(query = "xyz", queryMatches = 0, shown = 0, loading = true))
        assertEquals(BittensorSubnetEmptyKind.NONE, emptyKind(query = "xyz", queryMatches = 0, shown = 0, failed = true))
    }

    @Test
    fun `picker change is signed with one decimal`() {
        assertEquals("+48.5%", bittensorPickerChangePercent(java.math.BigDecimal("48.4987")))
        assertEquals("-3.2%", bittensorPickerChangePercent(java.math.BigDecimal("-3.21")))
        assertEquals("+16%", bittensorPickerChangePercent(java.math.BigDecimal("16.04")))
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

    private fun emptyKind(
        query: String,
        queryMatches: Int,
        shown: Int,
        catalogueSize: Int = 64,
        loading: Boolean = false,
        failed: Boolean = false
    ) = bittensorSubnetEmptyKind(loading, failed, query, catalogueSize, queryMatches, shown)

    private fun row(id: Int) = BittensorSubnetRowUi(
        subnet = BittensorSubnet(BittensorNetuid.Subnet(id), "Subnet $id", "alpha", null),
        metaId = 7L,
        favorite = false,
        price = "0.01 ${symbol.value}"
    )
}
