package io.novafoundation.nova.common.utils.recyclerView.expandable

import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableBaseItem
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableChildItem
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableParentItem

interface ExpandableBaseViewHolder<T : ExpandableBaseItem> {
    var expandableItem: T?
}

/**
 * The view holder that may show ExpandableChildItem's
 * It's used to check the type of viewHolder in [ExpandableItemDecoration]
 */
interface ExpandableParentViewHolder : ExpandableBaseViewHolder<ExpandableParentItem>

/**
 * The view holder that is shown as an ExpandableChildItem
 * It's used to check the type of viewHolder in [ExpandableItemDecoration]
 */
interface ExpandableChildViewHolder : ExpandableBaseViewHolder<ExpandableChildItem>

