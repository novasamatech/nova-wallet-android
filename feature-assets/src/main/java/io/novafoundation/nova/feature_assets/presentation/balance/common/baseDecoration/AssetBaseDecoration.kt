package io.novafoundation.nova.feature_assets.presentation.balance.common.baseDecoration

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.view.children
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_assets.R
import kotlin.math.roundToInt

/**
 * Note - clients are required to call [RecyclerView.invalidateItemDecorations] in [ListAdapter.submitList]  callback due to issues with DiffUtil.
 * The issue is that this decoration does not currently support partial list updates and assumes it will be iterated over whole list
 * TODO update decoration to not require this invalidation
 */
class AssetBaseDecoration(
    private val background: Drawable,
    private val assetsAdapter: ListAdapter<*, *>,
    context: Context,
    private val preferences: AssetDecorationPreferences
) : RecyclerView.ItemDecoration() {

    companion object;

    private val bounds = Rect()

    // used to hide rounded corners for the last group to simulate effect of not-closed group
    private val finalGroupExtraPadding = 20.dp(context)

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (assetsAdapter.itemCount == 0) return

        var groupTop: Int? = null

        parent.children.forEachIndexed { index, view ->
            val viewHolder = parent.getChildViewHolder(view)

            if (shouldSkip(viewHolder)) return@forEachIndexed

            val bindingPosition = viewHolder.bindingAdapterPosition

            val nextType = assetsAdapter.getItemViewTypeOrNull(bindingPosition + 1)

            if (groupTop == null) {
                parent.getDecoratedBoundsWithMargins(view, bounds)
                groupTop = bounds.top + view.translationY.roundToInt()
            }

            when {
                // if group is finished
                isFinalItemInGroup(nextType) -> {
                    parent.getDecoratedBoundsWithMargins(view, bounds)
                    bounds.set(view.left, bounds.top, view.right, bounds.bottom)

                    val groupBottom = bounds.bottom + view.translationY.roundToInt() - preferences.outerGroupPadding(viewHolder)

                    background.setBounds(bounds.left, groupTop!!, bounds.right, groupBottom)
                    background.draw(c)

                    if (index + 1 < parent.childCount) {
                        val nextView = parent.getChildAt(index + 1)
                        parent.getDecoratedBoundsWithMargins(nextView, bounds)

                        groupTop = bounds.top + view.translationY.roundToInt()
                    }
                }
                // draw last group
                index == parent.childCount - 1 -> {
                    parent.getDecoratedBoundsWithMargins(view, bounds)
                    bounds.set(view.left, bounds.top, view.right, bounds.bottom)

                    val groupBottom = bounds.bottom + view.translationY.roundToInt() + finalGroupExtraPadding
                    background.setBounds(bounds.left, groupTop!!, bounds.right, groupBottom)
                    background.draw(c)
                }
            }
        }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val viewHolder = parent.getChildViewHolder(view)

        if (shouldSkip(viewHolder)) {
            outRect.set(0, 0, 0, 0)

            return
        }

        val adapterPosition = viewHolder.bindingAdapterPosition

        val nextType = assetsAdapter.getItemViewTypeOrNull(adapterPosition + 1)

        val bottom = if (isFinalItemInGroup(nextType)) {
            preferences.outerGroupPadding(viewHolder) + preferences.innerGroupPadding(viewHolder)
        } else {
            0
        }

        outRect.set(0, 0, 0, bottom)
    }

    private fun RecyclerView.Adapter<*>.getItemViewTypeOrNull(position: Int): Int? {
        if (position < 0 || position >= itemCount) return null

        return getItemViewType(position)
    }

    private fun isFinalItemInGroup(nextType: Int?): Boolean {
        return nextType == null || preferences.isGroupItem(nextType)
    }

    private fun shouldSkip(viewHolder: RecyclerView.ViewHolder): Boolean {
        val noPosition = viewHolder.bindingAdapterPosition == RecyclerView.NO_POSITION
        val unsupportedViewHolder = !preferences.shouldUseViewHolder(viewHolder)

        return noPosition || unsupportedViewHolder
    }
}

fun AssetBaseDecoration.Companion.applyDefaultTo(
    recyclerView: RecyclerView,
    adapter: ListAdapter<*, *>,
    preferences: AssetDecorationPreferences = NetworkAssetDecorationPreferences()
) {
    val groupBackground = with(recyclerView.context) {
        addRipple(getRoundedCornerDrawable(R.color.block_background))
    }
    val decoration = AssetBaseDecoration(
        background = groupBackground,
        assetsAdapter = adapter,
        context = recyclerView.context,
        preferences = preferences
    )
    recyclerView.addItemDecoration(decoration)
}
