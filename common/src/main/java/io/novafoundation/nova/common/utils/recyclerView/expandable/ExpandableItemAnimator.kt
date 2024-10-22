package io.novafoundation.nova.common.utils.recyclerView.expandable

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.SimpleItemAnimator
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimationItemState
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimator
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableParentItem

/**
 * Potential problems:
 * - If in one time we will run add and move animation or remove and move animation - one of the animation will be cancelled
 */
class ExpandableItemAnimator(
    private val adapter: ExpandableAdapter,
    private val settings: ExpandableAnimationSettings,
    private val expandableAnimator: ExpandableAnimator
) : SimpleItemAnimator() {

    private val addAnimations = mutableMapOf<ItemKey, List<ViewHolder>>()
    private val removeAnimations = mutableMapOf<ItemKey, List<ViewHolder>>()
    private val moveAnimations = mutableListOf<ViewHolder>()

    private val pendingAddAnimations = mutableSetOf<ViewHolder>()
    private val pendingRemoveAnimations = mutableSetOf<ViewHolder>()
    private val pendingMoveAnimations = mutableSetOf<ViewHolder>()

    init {
        addDuration = settings.duration
        removeDuration = settings.duration
        moveDuration = settings.duration

        supportsChangeAnimations = false
    }

    override fun animateAdd(holder: ViewHolder): Boolean {
        if (holder !is ExpandableChildViewHolder) return false
        val item = holder.expandableItem ?: return false

        val parentItem = adapter.getItems().firstOrNull { it.getId() == item.groupId } as? ExpandableParentItem ?: return false

        if (pendingRemoveAnimations.contains(holder)) {
            holder.itemView.animate().cancel()
            pendingRemoveAnimations.remove(holder)
        } else {
            holder.itemView.alpha = 0f
            holder.itemView.scaleX = 0.90f
            holder.itemView.scaleY = 0.90f
        }

        val itemKey = parentItem.toKey()
        val currentViewHolders = addAnimations[itemKey] ?: emptyList()
        addAnimations[itemKey] = currentViewHolders + listOf(holder)

        expandableAnimator.prepareAnimationToState(parentItem, ExpandableAnimationItemState.Type.EXPANDING)

        return true
    }

    override fun animateRemove(holder: ViewHolder): Boolean {
        if (holder !is ExpandableChildViewHolder) return false
        val item = holder.expandableItem ?: return false

        val parentItem = adapter.getItems().firstOrNull { it.getId() == item.groupId } as? ExpandableParentItem ?: return false

        if (pendingAddAnimations.contains(holder)) {
            holder.itemView.animate().cancel()
            pendingAddAnimations.remove(holder)
        } else {
            holder.itemView.alpha = 1f
            holder.itemView.scaleX = 1f
            holder.itemView.scaleY = 1f
        }

        val itemKey = parentItem.toKey()
        val currentViewHolders = removeAnimations[itemKey] ?: emptyList()
        removeAnimations[itemKey] = currentViewHolders + listOf(holder)

        expandableAnimator.prepareAnimationToState(parentItem, ExpandableAnimationItemState.Type.COLLAPSING)

        return true
    }

    override fun animateMove(holder: ViewHolder, fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
        if (holder !is ExpandableBaseViewHolder<*>) return false

        if (pendingMoveAnimations.contains(holder)) {
            holder.itemView.animate().cancel()
            pendingMoveAnimations.remove(holder)
        }

        val yDelta = toY - fromY - holder.itemView.translationY
        holder.itemView.translationY = -yDelta
        moveAnimations.add(holder)
        return true
    }

    override fun animateChange(oldHolder: ViewHolder?, newHolder: ViewHolder?, fromLeft: Int, fromTop: Int, toLeft: Int, toTop: Int): Boolean {
        return false
    }

    override fun runPendingAnimations() {
        runAnimationFor(addAnimations, pendingAddAnimations) { animateAddImpl(it) }
        runAnimationFor(removeAnimations, pendingRemoveAnimations) { animateRemoveImpl(it) }

        val animatingViewHolders = moveAnimations.toList()
        moveAnimations.clear()

        for (holder in animatingViewHolders) {
            animateMoveImpl(holder)
        }

        pendingMoveAnimations.addAll(animatingViewHolders)
    }

    private fun runAnimationFor(
        animationGroup: MutableMap<ItemKey, List<ViewHolder>>,
        pendingAnimations: MutableSet<ViewHolder>,
        runAnimation: (ViewHolder) -> Unit
    ) {
        val parentItems = animationGroup.keys.toList()
        val animatingViewHolders = animationGroup.flatMap { (_, viewHolders) -> viewHolders }
        animationGroup.clear()

        parentItems.forEach { expandableAnimator.runAnimationFor(it.item) }
        for (holder in animatingViewHolders) {
            runAnimation(holder)
        }

        pendingAnimations.addAll(animatingViewHolders)
    }

    fun animateAddImpl(holder: ViewHolder) {
        val view = holder.itemView
        val animation = view.animate()
        animation.alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setInterpolator(settings.interpolator)
            .setDuration(settings.duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animator: Animator) {
                    addFinished(holder)
                    animation.setListener(null)
                }
            }).start()
    }

    fun animateRemoveImpl(holder: ViewHolder) {
        val view = holder.itemView
        val animation = view.animate()
        animation.alpha(0f)
            .scaleX(0.90f)
            .scaleY(0.90f)
            .setInterpolator(settings.interpolator)
            .setDuration(settings.duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animator: Animator) {
                    removeFinished(holder)
                    animation.setListener(null)
                }
            }).start()
    }

    fun animateMoveImpl(holder: ViewHolder) {
        val view = holder.itemView
        val animation = view.animate()
        animation.translationY(0f)
            .setDuration(settings.duration)
            .setInterpolator(settings.interpolator)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animator: Animator) {
                    moveFinished(holder)
                    animation.setListener(null)
                }
            }).start()
    }

    override fun endAnimation(viewHolder: ViewHolder) {
        viewHolder.itemView.animate().cancel()

        viewHolder.itemView.translationY = 0f
        viewHolder.itemView.alpha = 0f
        viewHolder.itemView.alpha = 1f
        viewHolder.itemView.scaleX = 1f
        viewHolder.itemView.scaleY = 1f
    }

    override fun endAnimations() {
        pendingAddAnimations.forEach { it.itemView.animate().cancel() }
        pendingAddAnimations.clear()

        pendingRemoveAnimations.forEach { it.itemView.animate().cancel() }
        pendingRemoveAnimations.clear()

        pendingMoveAnimations.forEach { it.itemView.animate().cancel() }
        pendingMoveAnimations.clear()

        addAnimations.clear()
        removeAnimations.clear()
        moveAnimations.clear()
    }

    override fun isRunning(): Boolean {
        return addAnimations.isNotEmpty() ||
            removeAnimations.isNotEmpty() ||
            moveAnimations.isNotEmpty() ||
            pendingAddAnimations.isNotEmpty() ||
            pendingRemoveAnimations.isNotEmpty() ||
            pendingMoveAnimations.isNotEmpty()
    }

    private fun addFinished(holder: ViewHolder) {
        pendingAddAnimations.remove(holder)
        internalDispatchAnimationFinished(holder)
    }

    private fun removeFinished(holder: ViewHolder) {
        pendingRemoveAnimations.remove(holder)
        internalDispatchAnimationFinished(holder)
    }

    private fun moveFinished(holder: ViewHolder) {
        pendingMoveAnimations.remove(holder)
        internalDispatchAnimationFinished(holder)
    }

    private fun internalDispatchAnimationFinished(holder: ViewHolder) {
        if (holder in pendingAddAnimations) return
        if (holder in pendingRemoveAnimations) return
        if (holder in pendingMoveAnimations) return

        dispatchAnimationFinished(holder)
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
