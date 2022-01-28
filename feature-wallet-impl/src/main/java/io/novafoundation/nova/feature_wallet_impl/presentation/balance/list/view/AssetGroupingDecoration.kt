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
    private val context: Context
) : RecyclerView.ItemDecoration() {

    private val bounds = Rect()
    private val groupOuterSpacing = 8.dp(context)
    private val groupInnerSpacing = 4.dp(context)

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val adapter = (parent.adapter as ListAdapter<AssetModel, *>)

        if (adapter.itemCount == 0) return

        var groupTop = -1

        parent.children.forEachIndexed { index, view ->

            val viewHolder = parent.getChildViewHolder(view)

            val adapterPosition = viewHolder.adapterPosition

            if (adapterPosition == RecyclerView.NO_POSITION) {
                return@forEachIndexed
            }

            val currentChainId = adapter.currentList[adapterPosition].token.configuration.chainId
            val nextChainId = adapter.currentList.getOrNull(adapterPosition + 1)?.token?.configuration?.chainId

            if (groupTop == -1) {
                parent.getDecoratedBoundsWithMargins(view, bounds)
                groupTop = bounds.top + view.translationY.roundToInt()
            }

            if (currentChainId != nextChainId) {
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
        }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val viewHolder = parent.getChildViewHolder(view)

        val adapterPosition = viewHolder.adapterPosition

        val adapter = (parent.adapter as ListAdapter<AssetModel, *>)

        val previousChainId = adapter.currentList.getOrNull(adapterPosition - 1)?.token?.configuration?.chainId
        val currentChainId = adapter.currentList[adapterPosition].token.configuration.chainId
        val nextChainId = adapter.currentList.getOrNull(adapterPosition + 1)?.token?.configuration?.chainId

        val top = if (previousChainId != currentChainId) groupInnerSpacing else 0
        val bottom = if (nextChainId != currentChainId) groupInnerSpacing + groupOuterSpacing else 0

        outRect.set(0, top, 0, bottom)
    }
}
