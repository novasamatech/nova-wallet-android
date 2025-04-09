package io.novafoundation.nova.common.utils.recyclerView.space

import android.view.View
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.getAdapterPosition(view: View): Int? {
    val adapterPosition = getChildAdapterPosition(view)
    if (adapterPosition == RecyclerView.NO_POSITION) return null
    return adapterPosition
}

fun RecyclerView.getViewType(view: View): Int? {
    val adapterPosition = getAdapterPosition(view) ?: return null

    return getViewTypeForPosition(adapterPosition)
}

fun RecyclerView.getNextViewType(view: View): Int? {
    val adapterPosition = getAdapterPosition(view) ?: return null

    return getViewTypeForPosition(adapterPosition + 1)
}

fun RecyclerView.getViewTypeForPosition(position: Int): Int? {
    if (position == RecyclerView.NO_POSITION) return null

    return adapter?.getViewTypeInNestedAdapters(position)
}

private fun RecyclerView.Adapter<*>.getViewTypeInNestedAdapters(position: Int): Int? {
    if (position >= itemCount) return null

    return when (this) {
        is ConcatAdapter -> this.getTrueViewTypeFromPosition(position)

        else -> this.getItemViewType(position)
    }
}

/*
ConcatAdapter may change view types of nested adapters, so this method returns true view type of a position
 */
private fun ConcatAdapter.getTrueViewTypeFromPosition(position: Int): Int? {
    var localPosition = position
    adapters.forEach {
        if (localPosition < it.itemCount) {
            return it.getViewTypeInNestedAdapters(localPosition)
        } else {
            localPosition -= it.itemCount
        }
    }

    return null
}
