package io.novafoundation.nova.common.utils.recyclerView.expandable

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimationItemState
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableBaseItem

fun ExpandableAnimationItemState.flippedFraction(): Float {
    return 1f - animationFraction
}

fun Float.flippedFraction(): Float {
    return 1f - this
}

fun ExpandableAnimationItemState.expandingFraction(): Float {
    return when (animationType) {
        ExpandableAnimationItemState.Type.EXPANDING -> animationFraction
        ExpandableAnimationItemState.Type.COLLAPSING -> flippedFraction()
    }
}

fun ExpandableAdapter.getItemFor(viewHolder: ViewHolder): ExpandableBaseItem? {
    if (viewHolder !is ExpandableViewHolder) return null

    return getItems().getOrNull(viewHolder.absoluteAdapterPosition - 1)
}
