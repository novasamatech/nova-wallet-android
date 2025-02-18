package io.novafoundation.nova.common.utils.recyclerView.dragging

import androidx.recyclerview.widget.RecyclerView.ViewHolder

interface StartDragListener {
    fun requestDrag(viewHolder: ViewHolder)
}
