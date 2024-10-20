package io.novafoundation.nova.common.utils.recyclerView.expandable

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import androidx.recyclerview.widget.RecyclerView.ItemAnimator
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.findIndexByStep
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimator
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimationItemState
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableChildItem
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableParentItem


class ExpandableItemAnimator(
    private val adapter: ExpandableAdapter,
    private val settings: ExpandableAnimationSettings,
    private val expandableAnimator: ExpandableAnimator
) : ItemAnimator() {

    private var isRunning = false

    private val pendingAnimations = mutableMapOf<ItemKey, List<ViewHolder>>() // Item id to ViewHolder

    init {
        addDuration = settings.duration
        moveDuration = 500
        changeDuration = 500
    }

    override fun animateDisappearance(viewHolder: ViewHolder, preLayoutInfo: ItemHolderInfo, postLayoutInfo: ItemHolderInfo?): Boolean {
        viewHolder.itemView.alpha = 0f
        return true
    }

    override fun animateAppearance(viewHolder: ViewHolder, preLayoutInfo: ItemHolderInfo?, postLayoutInfo: ItemHolderInfo): Boolean {
        val item = adapter.getItemFor(viewHolder)

        if (item !is ExpandableChildItem) return false


        val (parentPosition, parentItem) = findParentFor(viewHolder) ?: return false

        viewHolder.itemView.alpha = 0f
        viewHolder.itemView.scaleX = 0.7f
        viewHolder.itemView.scaleY = 0.7f

        val position = viewHolder.absoluteAdapterPosition - parentPosition
        //viewHolder.itemView.translationY = 50f - viewHolder.itemView.height / 2f * 0.7f * position
        val itemKey = parentItem.toKey()
        val currentViewHolders = pendingAnimations[itemKey] ?: emptyList()
        pendingAnimations[itemKey] = currentViewHolders + listOf(viewHolder)
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
        val parentItems = pendingAnimations.keys.toList()
        val animatingViewHolders = pendingAnimations.flatMap { (_, viewHolders) -> viewHolders }
        pendingAnimations.clear()
        val adder = Runnable {
            for (holder in animatingViewHolders) {
                animateAddImpl(holder)
            }
        }

        parentItems.forEach { expandableAnimator.animateItemToState(it.item, ExpandableAnimationItemState.Type.EXPANDING) }
        adder.run()
    }

    fun animateAddImpl(holder: ViewHolder) {
        val view = holder.itemView
        val animation = view.animate()
        animation.alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(settings.duration)
            .translationY(0f)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animator: Animator) {
                    isRunning = true
                }

                override fun onAnimationCancel(animator: Animator) {
                    isRunning = false
                    view.alpha = 1f
                    view.scaleX = 1f
                }

                override fun onAnimationEnd(animator: Animator) {
                    isRunning = false
                    animation.setListener(null)
                }
            }).start()
    }

    override fun endAnimation(viewHolder: ViewHolder) {
        // Nothing to do
    }

    override fun endAnimations() {
        expandableAnimator.cancelAnimations()
    }

    override fun isRunning(): Boolean {
        return isRunning
    }

    private fun findParentFor(viewHolder: ViewHolder): Pair<Int, ExpandableParentItem>? {
        val items = adapter.getItems()
        val adapterPosition = viewHolder.absoluteAdapterPosition
        val groupPosition = items.findIndexByStep(fromPosition = adapterPosition, -1) { it is ExpandableParentItem } ?: return null

        return groupPosition to items[groupPosition] as ExpandableParentItem
    }
}

private class ItemKey(val item: ExpandableParentItem) {

    override fun equals(other: Any?): Boolean {
        if (other !is ItemKey) return false
        return item.getId() == other.item.getId()
    }

    override fun hashCode(): Int {
        return item.hashCode()
    }
}

private fun ExpandableParentItem.toKey(): ItemKey {
    return ItemKey(this)
}
