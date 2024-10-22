package io.novafoundation.nova.common.utils.recyclerView.expandable

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.util.Log
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.SimpleItemAnimator
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimationItemState
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimator
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableParentItem

/**
 * Potential problems:
 * - If in one time point we have add and move animation or remove and move animation and we cancel move animation - the add or remove animation will be also canceled
 */
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
            holder.itemView.scaleX = 0.90f
            holder.itemView.scaleY = 0.90f
        }

        //val position = viewHolder.absoluteAdapterPosition - parentPosition
        //viewHolder.itemView.translationY = 50f - viewHolder.itemView.height / 2f * 0.7f * position
        val itemKey = parentItem.toKey()
        val currentViewHolders = addAnimations[itemKey] ?: emptyList()
        addAnimations[itemKey] = currentViewHolders + listOf(holder)

        expandableAnimator.animateItemToState(parentItem, ExpandableAnimationItemState.Type.EXPANDING)

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

        expandableAnimator.animateItemToState(parentItem, ExpandableAnimationItemState.Type.COLLAPSING)

        return true
    }

    override fun animateMove(holder: ViewHolder, fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
        if (holder !is ExpandableBaseViewHolder<*>) return false

        if (pendingMoveAnimations.contains(holder)) {
            holder.itemView.animate().cancel()
            pendingMoveAnimations.remove(holder)
        } else {
            //val yDelta = toY - fromY
            //holder.itemView.translationY = -yDelta.toFloat()
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

        //parentItems.forEach { expandableAnimator.animateItemToState(it.item, toState) }
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
                    //addFinished(holder)
                }

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
            .setDuration(settings.duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animator: Animator) {}

                override fun onAnimationCancel(animator: Animator) {
                    //removeFinished(holder)
                }

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
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animator: Animator) {}

                override fun onAnimationCancel(animator: Animator) {
                    //moveFinished(holder)
                }

                override fun onAnimationEnd(animator: Animator) {
                    moveFinished(holder)
                    animation.setListener(null)
                }
            }).start()
    }

    override fun endAnimation(viewHolder: ViewHolder) {
        Log.d("ExpandableItemAnimator", "add:" + addAnimations.size.toString())
        Log.d("ExpandableItemAnimator", "remove:" + removeAnimations.size.toString())
        Log.d("ExpandableItemAnimator", "move:" + moveAnimations.size.toString())

        viewHolder.itemView.clearAnimation()

        if (pendingAddAnimations.remove(viewHolder)) {
            viewHolder.itemView.alpha = 1f
        }
        if (pendingRemoveAnimations.remove(viewHolder)) {
            viewHolder.itemView.alpha = 0f
        }
        if (pendingMoveAnimations.remove(viewHolder)) {
            viewHolder.itemView.translationY = 0f
        }

        dispatchAnimationFinished(viewHolder)
    }

    override fun endAnimations() {
        pendingAddAnimations.forEach {
            it.itemView.clearAnimation()
            dispatchAddFinished(it)
        }
        pendingAddAnimations.clear()

        pendingRemoveAnimations.forEach {
            it.itemView.clearAnimation()
            dispatchRemoveFinished(it)
        }
        pendingRemoveAnimations.clear()

        pendingMoveAnimations.forEach {
            it.itemView.clearAnimation()
            dispatchMoveFinished(it)
        }
        pendingMoveAnimations.clear()

        addAnimations.clear()
        removeAnimations.clear()
        moveAnimations.clear()

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
