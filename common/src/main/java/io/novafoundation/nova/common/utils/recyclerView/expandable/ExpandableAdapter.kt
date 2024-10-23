package io.novafoundation.nova.common.utils.recyclerView.expandable

import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableBaseItem

interface ExpandableAdapter {

    fun getItems(): List<ExpandableBaseItem>
}
