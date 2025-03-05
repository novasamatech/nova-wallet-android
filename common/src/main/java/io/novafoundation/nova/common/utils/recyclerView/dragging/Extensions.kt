package io.novafoundation.nova.common.utils.recyclerView.dragging

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView.ViewHolder

@SuppressLint("ClickableViewAccessibility")
fun View.prepareForDragging(viewHolder: ViewHolder, startDragListener: StartDragListener) {
    this.setOnTouchListener { _, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
            startDragListener.requestDrag(viewHolder)
        }
        false
    }
}
