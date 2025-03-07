package io.novafoundation.nova.common.utils.recyclerView.space

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.recyclerView.ViewType

class SpaceBetween(
    private val upperViewType: ViewType,
    private val lowerViewType: ViewType,
    private val spaceDp: Int
) : RecyclerViewItemSpace {

    override fun handleSpace(outRect: Rect, view: View, parent: RecyclerView): Boolean {
        if (shouldSetSpaceForItems(parent, view)) {
            setSpaceBetweenItems(outRect, view)
            return true
        }

        return false
    }

    private fun shouldSetSpaceForItems(
        parent: RecyclerView,
        view: View
    ): Boolean {
        parent.getViewType(view).takeIf { it == upperViewType } ?: return false
        parent.getNextViewType(view).takeIf { it == lowerViewType } ?: return false

        return true
    }

    private fun setSpaceBetweenItems(outRect: Rect, view: View) {
        outRect.set(0, 0, 0, spaceDp.dp(view.context))
    }
}
