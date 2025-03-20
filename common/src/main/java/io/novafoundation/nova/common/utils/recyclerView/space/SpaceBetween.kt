package io.novafoundation.nova.common.utils.recyclerView.space

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.recyclerView.WithViewType

class SpaceBetween(
    upperViewTypeProvider: WithViewType,
    lowerViewTypeProvider: WithViewType,
    private val spaceDp: Int
) : RecyclerViewItemSpace {

    private val upperViewType = upperViewTypeProvider.viewType
    private val lowerViewType = lowerViewTypeProvider.viewType

    constructor(singleViewTypeProvider: WithViewType, spaceDp: Int) : this(singleViewTypeProvider, singleViewTypeProvider, spaceDp)

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
        if (parent.getViewType(view) != upperViewType) return false
        if (parent.getNextViewType(view) != lowerViewType) return false

        return true
    }

    private fun setSpaceBetweenItems(outRect: Rect, view: View) {
        outRect.set(0, 0, 0, spaceDp.dp(view.context))
    }
}
