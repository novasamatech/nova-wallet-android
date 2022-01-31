package io.novafoundation.nova.feature_wallet_impl.presentation.balance.list.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.view.children
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.feature_wallet_impl.presentation.model.AssetModel
import kotlin.math.roundToInt

class AssetGroupingDecoration(
    private val background: Drawable,
    private val assetsAdapter: ListAdapter<AssetModel, *>,
    context: Context,
) : RecyclerView.ItemDecoration() {

    private val bounds = Rect()
    private val groupOuterSpacing = 8.dp(context)
    private val groupInnerSpacing = 4.dp(context)

    // used to hide rounded corners for the last group to simulate effect of not-closed group
    private val finalGroupExtraPadding = 20.dp(context)

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (assetsAdapter.itemCount == 0) return

        var groupTop = -1

        parent.children.forEachIndexed { index, view ->
            val viewHolder = parent.getChildViewHolder(view)

            if (shouldSkip(viewHolder)) return@forEachIndexed

            val bindingPosition = viewHolder.bindingAdapterPosition

            val currentChainId = assetsAdapter.currentList[bindingPosition].token.configuration.chainId
            val nextChainId = assetsAdapter.currentList.getOrNull(bindingPosition + 1)?.token?.configuration?.chainId

            if (groupTop == -1) {
                parent.getDecoratedBoundsWithMargins(view, bounds)
                groupTop = bounds.top + view.translationY.roundToInt()
            }

            when {
                // if group changed
                currentChainId != nextChainId -> {
                    parent.getDecoratedBoundsWithMargins(view, bounds)
                    val groupBottom = bounds.bottom + view.translationY.roundToInt() - groupOuterSpacing

                    background.setBounds(bounds.left, groupTop, bounds.right, groupBottom)
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

                    val groupBottom = bounds.bottom + view.translationY.roundToInt() + finalGroupExtraPadding
                    background.setBounds(bounds.left, groupTop, bounds.right, groupBottom)
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

        val previousChainId = assetsAdapter.currentList.getOrNull(adapterPosition - 1)?.token?.configuration?.chainId
        val currentChainId = assetsAdapter.currentList[adapterPosition].token.configuration.chainId
        val nextChainId = assetsAdapter.currentList.getOrNull(adapterPosition + 1)?.token?.configuration?.chainId

        val top = if (previousChainId != currentChainId) groupInnerSpacing else 0
        val bottom = if (nextChainId != currentChainId) groupInnerSpacing + groupOuterSpacing else 0

        outRect.set(0, top, 0, bottom)
    }

    private fun shouldSkip(viewHolder: RecyclerView.ViewHolder): Boolean {
        return viewHolder.bindingAdapterPosition == RecyclerView.NO_POSITION || viewHolder !is AssetViewHolder
    }
}
