package io.novafoundation.nova.feature_assets.presentation.balance.common

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemAnimator
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.findIndexByStep
import io.novafoundation.nova.feature_assets.presentation.balance.common.holders.TokenAssetViewHolder
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi

class AssetTokensItemAnimator(
    private val adapter: BalanceListAdapter,
    private val decoration: AssetTokensDecoration,
    private val recyclerView: BalanceListRecyclerView
) : ItemAnimator() {

    private var isRunning = false

    private val pendingAnimations = mutableListOf<ViewHolder>()

    init {
        addDuration = 500
        moveDuration = 500
        changeDuration = 500
    }

    override fun onAnimationFinished(viewHolder: ViewHolder) {
        recyclerView.invalidate()
    }


    override fun animateDisappearance(viewHolder: ViewHolder, preLayoutInfo: ItemHolderInfo, postLayoutInfo: ItemHolderInfo?): Boolean {
        viewHolder.itemView.alpha = 0f
        return true
    }

    override fun animateAppearance(viewHolder: ViewHolder, preLayoutInfo: ItemHolderInfo?, postLayoutInfo: ItemHolderInfo): Boolean {
        if (viewHolder !is TokenAssetViewHolder) return false
        viewHolder.itemView.alpha = 0f
        viewHolder.itemView.scaleX = 0.7f
        viewHolder.itemView.scaleY = 0.7f

        val adapterPosition = viewHolder.absoluteAdapterPosition
        val groupPosition = adapter.currentList.findIndexByStep(fromPosition = adapterPosition, -1) { it is TokenGroupUi } ?: return false
        val position = viewHolder.absoluteAdapterPosition - groupPosition
        viewHolder.itemView.translationY = 50f - viewHolder.itemView.height / 2f * 0.7f * position
        pendingAnimations.add(viewHolder)
        return true
    }

    override fun animatePersistence(viewHolder: ViewHolder, preLayoutInfo: ItemHolderInfo, postLayoutInfo: ItemHolderInfo): Boolean {
        return false
    }

    override fun animateChange(
        oldHolder: ViewHolder,
        newHolder: ViewHolder,
        preLayoutInfo: ItemHolderInfo,
        postLayoutInfo: ItemHolderInfo
    ): Boolean {
        return true
    }

    override fun runPendingAnimations() {
        val additions = ArrayList<ViewHolder>()
        additions.addAll(pendingAnimations)
        pendingAnimations.clear()
        val adder = Runnable {
            for (holder in additions) {
                animateAddImpl(holder)
            }
            additions.clear()
        }

        adder.run()
    }

    fun animateAddImpl(holder: ViewHolder) {
        val view = holder.itemView
        val animation = view.animate()
        animation.alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .translationY(0f)
            .setUpdateListener {
                recyclerView.invalidate()
            }
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animator: Animator) {
                    recyclerView.invalidate()
                    isRunning = true
                }

                override fun onAnimationCancel(animator: Animator) {
                    recyclerView.invalidate()
                    isRunning = false
                    view.alpha = 1f
                    view.scaleX = 1f
                }

                override fun onAnimationEnd(animator: Animator) {
                    recyclerView.invalidate()
                    isRunning = false
                    animation.setListener(null)
                }
            }).start()
    }

    override fun endAnimation(item: ViewHolder) {

    }

    override fun endAnimations() {
    }

    override fun isRunning(): Boolean {
        return isRunning
    }
}

