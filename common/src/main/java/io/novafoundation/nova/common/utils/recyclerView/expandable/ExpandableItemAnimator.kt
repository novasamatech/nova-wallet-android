package io.novafoundation.nova.common.utils.recyclerView.expandable

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.SimpleItemAnimator
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimationItemState
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimator
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableParentItem


class ExpandableItemAnimator(
    private val adapter: ExpandableAdapter,
    private val settings: ExpandableAnimationSettings,
    private val expandableAnimator: ExpandableAnimator
) : SimpleItemAnimator() {

    private val addAnimations = mutableMapOf<ItemKey, List<ViewHolder>>() // Item id to ViewHolder
    private val removeAnimations = mutableMapOf<ItemKey, List<ViewHolder>>() // Item id to ViewHolder
    private val moveAnimations = mutableListOf<ViewHolder>() // Item id to ViewHolder

    private val pendingAddAnimations = mutableSetOf<ViewHolder>() // Item id to ViewHolder
    private val pendingRemoveAnimations = mutableSetOf<ViewHolder>() // Item id to ViewHolder
    private val pendingMoveAnimations = mutableSetOf<ViewHolder>() // Item id to ViewHolder

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
            holder.itemView.scaleX = 0.95f
            holder.itemView.scaleY = 0.95f
        }

        //val position = viewHolder.absoluteAdapterPosition - parentPosition
        //viewHolder.itemView.translationY = 50f - viewHolder.itemView.height / 2f * 0.7f * position
        val itemKey = parentItem.toKey()
        val currentViewHolders = addAnimations[itemKey] ?: emptyList()
        addAnimations[itemKey] = currentViewHolders + listOf(holder)
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
            holder.itemView.translationY = 0f
        }

        val itemKey = parentItem.toKey()
        val currentViewHolders = removeAnimations[itemKey] ?: emptyList()
        removeAnimations[itemKey] = currentViewHolders + listOf(holder)
        return true
    }

    override fun animateMove(holder: ViewHolder, fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
        if (pendingMoveAnimations.contains(holder)) {
            //holder.itemView.animate().cancel()
            //pendingMoveAnimations.remove(holder)
        } else {
            val yDelta = toY - fromY
            holder.itemView.translationY = -yDelta.toFloat()
        }

        val yDelta = toY - fromY
        holder.itemView.translationY = -yDelta.toFloat()
        moveAnimations.add(holder)
        return true
    }

    override fun animateChange(oldHolder: ViewHolder?, newHolder: ViewHolder?, fromLeft: Int, fromTop: Int, toLeft: Int, toTop: Int): Boolean {
        return false
    }

    override fun runPendingAnimations() {
        runAnimationFor(addAnimations, pendingAddAnimations, ExpandableAnimationItemState.Type.EXPANDING) { animateAddImpl(it) }
        runAnimationFor(removeAnimations, pendingRemoveAnimations, ExpandableAnimationItemState.Type.COLLAPSING) { animateRemoveImpl(it) }

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
        toState: ExpandableAnimationItemState.Type,
        runAnimation: (ViewHolder) -> Unit
    ) {
        val parentItems = animationGroup.keys.toList()
        val animatingViewHolders = animationGroup.flatMap { (_, viewHolders) -> viewHolders }
        animationGroup.clear()

        parentItems.forEach { expandableAnimator.animateItemToState(it.item, toState) }
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
            .setDuration(settings.duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animator: Animator) {}

                override fun onAnimationCancel(animator: Animator) {
                    dispatchAnimationFinished(holder)
                    pendingAddAnimations.remove(holder)
                    view.alpha = 1f
                    view.scaleX = 1f
                }

                override fun onAnimationEnd(animator: Animator) {
                    dispatchAnimationFinished(holder)
                    pendingAddAnimations.remove(holder)
                    animation.setListener(null)
                }
            }).start()
    }

    fun animateRemoveImpl(holder: ViewHolder) {
        val view = holder.itemView
        val animation = view.animate()
        animation.alpha(0f)
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(settings.duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animator: Animator) {}

                override fun onAnimationCancel(animator: Animator) {
                    dispatchAnimationFinished(holder)
                    pendingRemoveAnimations.remove(holder)
                    view.alpha = 0f
                    view.scaleX = 0.95f
                }

                override fun onAnimationEnd(animator: Animator) {
                    dispatchAnimationFinished(holder)
                    pendingRemoveAnimations.remove(holder)
                    animation.setListener(null)
                }
            }).start()
    }

    fun animateMoveImpl(holder: ViewHolder) {
        val view = holder.itemView
        val animation = view.animate()
        animation.translationY(0f)
            .setDuration(settings.duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animator: Animator) {}

                override fun onAnimationCancel(animator: Animator) {
                    dispatchAnimationFinished(holder)
                    pendingRemoveAnimations.remove(holder)
                    view.translationY = 0f
                }

                override fun onAnimationEnd(animator: Animator) {
                    dispatchAnimationFinished(holder)
                    pendingRemoveAnimations.remove(holder)
                    animation.setListener(null)
                }
            }).start()
    }

    override fun endAnimation(viewHolder: ViewHolder) {
        viewHolder.itemView.clearAnimation()
        dispatchAnimationFinished(viewHolder)
        pendingAddAnimations.remove(viewHolder)
        pendingRemoveAnimations.remove(viewHolder)
        pendingMoveAnimations.remove(viewHolder)
    }

    override fun endAnimations() {
        pendingAddAnimations.forEach {
            it.itemView.clearAnimation()
            dispatchAnimationFinished(it)
        }
        pendingAddAnimations.clear()

        pendingRemoveAnimations.forEach {
            it.itemView.clearAnimation()
            dispatchAnimationFinished(it)
        }
        pendingRemoveAnimations.clear()

        pendingMoveAnimations.forEach {
            it.itemView.clearAnimation()
            dispatchAnimationFinished(it)
        }
        pendingMoveAnimations.clear()

        expandableAnimator.cancelAnimations()
    }

    override fun isRunning(): Boolean {
        return addAnimations.isNotEmpty() ||
            removeAnimations.isNotEmpty() ||
            moveAnimations.isNotEmpty() ||
            pendingAddAnimations.isNotEmpty() ||
            pendingRemoveAnimations.isNotEmpty() ||
            pendingMoveAnimations.isNotEmpty()
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
