package io.novafoundation.nova.common.utils.recyclerView.space

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import io.novafoundation.nova.common.utils.recyclerView.ViewType

class SpaceItemDecoration(private val spaces: List<RecyclerViewItemSpace>) : ItemDecoration() {

    class Builder {
        private val spaces = mutableListOf<RecyclerViewItemSpace>()

        fun addSpaceBetween(
            upperViewType: ViewType,
            lowerViewType: ViewType,
            spaceDp: Int
        ) {
            spaces.add(SpaceBetween(upperViewType, lowerViewType, spaceDp))
        }

        fun addSpaceBetween(
            sameViewType: ViewType,
            spaceDp: Int
        ) {
            spaces.add(SpaceBetween(sameViewType, sameViewType, spaceDp))
        }

        fun build(): SpaceItemDecoration {
            return SpaceItemDecoration(spaces)
        }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        handleFirstMatchedSpaceFor(outRect, view, parent)
    }

    private fun handleFirstMatchedSpaceFor(outRect: Rect, view: View, parent: RecyclerView) {
        spaces.firstOrNull { it.handleSpace(outRect, view, parent) }
    }
}

fun RecyclerView.addSpaceItemDecoration(setup: SpaceItemDecoration.Builder.() -> Unit) {
    val builder = SpaceItemDecoration.Builder()
    builder.setup()
    addItemDecoration(builder.build())
}
