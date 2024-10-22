package io.novafoundation.nova.common.utils.recyclerView.expandable

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.ViewPropertyAnimator
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.SimpleItemAnimator
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimationItemState
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimator
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableChildItem
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableParentItem

/**
 * Potential problems:
 * - If in one time we will run add and move animation or remove and move animation - one of the animation will be cancelled
 */
abstract class ExpandableItemAnimator(
    private val adapter: ExpandableAdapter,
    private val settings: ExpandableAnimationSettings,
    private val expandableAnimator: ExpandableAnimator
) : SimpleItemAnimator() {

    private val addAnimations = mutableMapOf<ItemKey, MutableList<ViewHolder>>()
    private val removeAnimations = mutableMapOf<ItemKey, MutableList<ViewHolder>>()
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

        val parentItem = getParentFor(item) ?: return false

        if (pendingRemoveAnimations.contains(holder)) {
            holder.itemView.animate().cancel()
        } else {
            preAddImpl(holder)
        }

        val itemKey = parentItem.toKey()

        if (itemKey !in addAnimations) addAnimations[itemKey] = mutableListOf()
        addAnimations[itemKey]?.add(holder)

        expandableAnimator.prepareAnimationToState(parentItem, ExpandableAnimationItemState.Type.EXPANDING)

        return true
    }

    override fun animateRemove(holder: ViewHolder): Boolean {
        if (holder !is ExpandableChildViewHolder) return false
        val item = holder.expandableItem ?: return false

        val parentItem = getParentFor(item) ?: return false

        if (pendingAddAnimations.contains(holder)) {
            holder.itemView.animate().cancel()
        } else {
            preRemoveImpl(holder)
        }

        val itemKey = parentItem.toKey()

        if (itemKey !in removeAnimations) removeAnimations[itemKey] = mutableListOf()
        removeAnimations[itemKey]?.add(holder)

        expandableAnimator.prepareAnimationToState(parentItem, ExpandableAnimationItemState.Type.COLLAPSING)

        return true
    }

    override fun animateMove(holder: ViewHolder, fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
        if (holder !is ExpandableBaseViewHolder<*>) return false

        if (pendingMoveAnimations.contains(holder)) {
            holder.itemView.animate().cancel()
        }

        preMoveImpl(holder, fromY, toY)
        moveAnimations.add(holder)
        return true
    }

    override fun animateChange(oldHolder: ViewHolder?, newHolder: ViewHolder?, fromLeft: Int, fromTop: Int, toLeft: Int, toTop: Int): Boolean {
        return false
    }

    override fun runPendingAnimations() {
        //Add animation + expand items
        runExpandableAnimationFor(addAnimations, pendingAddAnimations) { animateAddImpl(it) }

        //Remove animation + collapse items
        runExpandableAnimationFor(removeAnimations, pendingRemoveAnimations) { animateRemoveImpl(it) }

        //Move animation
        val animatingViewHolders = moveAnimations.toList()
        moveAnimations.clear()

        for (holder in animatingViewHolders) {
            animateMoveImpl(holder)
        }

        pendingMoveAnimations.addAll(animatingViewHolders)
    }

    private fun runExpandableAnimationFor(
        animationGroup: MutableMap<ItemKey, MutableList<ViewHolder>>,
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

    abstract fun preAddImpl(holder: ViewHolder)

    abstract fun getAddAnimator(holder: ViewHolder): ViewPropertyAnimator

    abstract fun preRemoveImpl(holder: ViewHolder)

    abstract fun getRemoveAnimator(holder: ViewHolder): ViewPropertyAnimator

    abstract fun preMoveImpl(holder: ViewHolder, fromY: Int, toY: Int)

    abstract fun getMoveAnimator(holder: ViewHolder): ViewPropertyAnimator

    private fun animateAddImpl(holder: ViewHolder) {
        val animation = getAddAnimator(holder)
        animation.setInterpolator(settings.interpolator)
            .setDuration(settings.duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animator: Animator) {
                    addFinished(holder)
                    animation.setListener(null)
                }
            }).start()
    }

    private fun animateRemoveImpl(holder: ViewHolder) {
        val animation = getRemoveAnimator(holder)
        animation.setInterpolator(settings.interpolator)
            .setDuration(settings.duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animator: Animator) {
                    removeFinished(holder)
                    animation.setListener(null)
                }
            }).start()
    }

    private fun animateMoveImpl(holder: ViewHolder) {
        val animation = getMoveAnimator(holder)
        animation.setInterpolator(settings.interpolator)
            .setDuration(settings.duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animator: Animator) {
                    moveFinished(holder)
                    animation.setListener(null)
                }
            }).start()
    }

    override fun endAnimation(viewHolder: ViewHolder) {
        viewHolder.itemView.animate().cancel()
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

    private fun getParentFor(item: ExpandableChildItem): ExpandableParentItem? {
        return adapter.getItems().firstOrNull { it.getId() == item.groupId } as? ExpandableParentItem
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
