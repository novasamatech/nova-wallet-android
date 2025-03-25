package io.novafoundation.nova.common.utils.recyclerView.space

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

interface RecyclerViewItemSpace {
    fun handleSpace(outRect: Rect, view: View, parent: RecyclerView): Boolean
}
