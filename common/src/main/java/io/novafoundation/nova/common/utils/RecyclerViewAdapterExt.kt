package io.novafoundation.nova.common.utils

import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.ViewHolder.doIfPositionValid(block: (position: Int) -> Unit) {
    val position = bindingAdapterPosition
    if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
        block(position)
    }
}
