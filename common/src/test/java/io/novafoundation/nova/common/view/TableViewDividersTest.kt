package io.novafoundation.nova.common.view

import org.junit.Assert.assertEquals
import org.junit.Test

class TableViewDividersTest {

    private fun visible(allowsDivider: Boolean = true) = TableRowDividerState(isVisible = true, allowsDivider = allowsDivider)

    private fun gone(allowsDivider: Boolean = true) = TableRowDividerState(isVisible = false, allowsDivider = allowsDivider)

    @Test
    fun `dividers separate every pair of visible rows`() {
        assertEquals(listOf(0, 1), tableRowsWithDividerBelow(listOf(visible(), visible(), visible())))
    }

    @Test
    fun `a hidden last row leaves no divider under the last visible row`() {
        assertEquals(listOf(0), tableRowsWithDividerBelow(listOf(visible(), visible(), gone())))
        assertEquals(listOf(0), tableRowsWithDividerBelow(listOf(visible(), visible(), gone(), gone())))
    }

    @Test
    fun `a hidden middle row draws nothing and keeps its neighbours separated`() {
        assertEquals(listOf(0, 1), tableRowsWithDividerBelow(listOf(visible(), visible(), gone(), visible())))
        assertEquals(listOf(0), tableRowsWithDividerBelow(listOf(visible(), gone(), visible())))
    }

    @Test
    fun `rows that disable dividers are skipped`() {
        assertEquals(listOf(1), tableRowsWithDividerBelow(listOf(visible(allowsDivider = false), visible(), visible())))
    }

    @Test
    fun `no dividers for a single visible row or an empty table`() {
        assertEquals(emptyList<Int>(), tableRowsWithDividerBelow(listOf(gone(), visible(), gone())))
        assertEquals(emptyList<Int>(), tableRowsWithDividerBelow(listOf(gone(), gone())))
        assertEquals(emptyList<Int>(), tableRowsWithDividerBelow(emptyList()))
    }
}
