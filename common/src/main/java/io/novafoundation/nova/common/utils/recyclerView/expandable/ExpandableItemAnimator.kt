package io.novafoundation.nova.common.utils.recyclerView.expandable

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.ViewPropertyAnimator
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.SimpleItemAnimator
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimationItemState
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimator

/**
 * Potential problems:
 * - If in one time we will run add and move animation or remove and move animation - one of an animation will be cancelled
 */
abstract class ExpandableItemAnimator(
    private val settings: ExpandableAnimationSettings,
    private val expandableAnimator: ExpandableAnimator
) : SimpleItemAnimator() {

    private var preparedForAnimation = false

    private val addAnimations = mutableMapOf<String, MutableList<ViewHolder>>() // Parent item to children
    private val removeAnimations = mutableMapOf<String, MutableList<ViewHolder>>() // Parent item to children
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

    /**
     * Use this method before adapter.submitList() to prepare items for animation.
     * Item animations will be skipped otherwise
     */
    fun prepareForAnimation() {
        preparedForAnimation = true
    }

    override fun animateAdd(holder: ViewHolder): Boolean {
        val notPreparedForAnimation = !preparedForAnimation
        val notExpandableChildItem = holder !is ExpandableChildViewHolder || holder.expandableItem == null
        if (notPreparedForAnimation || notExpandableChildItem) {
            dispatchAddFinished(holder)
            return true
        }

        val item = (holder as ExpandableChildViewHolder).expandableItem!!

        // Reset move state helps clear translationY when animation is being to be canceled
        if (pendingMoveAnimations.contains(holder)) {
            holder.itemView.animate().cancel()

            resetMoveState(holder)
        }

        if (pendingRemoveAnimations.contains(holder)) {
            holder.itemView.animate().cancel()
        } else {
            preAddImpl(holder)
        }

        if (item.groupId !in addAnimations) addAnimations[item.groupId] = mutableListOf()
        addAnimations[item.groupId]?.add(holder)

        expandableAnimator.prepareAnimationToState(item.groupId, ExpandableAnimationItemState.Type.EXPANDING)

        return true
    }

    override fun animateRemove(holder: ViewHolder): Boolean {
        val notPreparedForAnimation = !preparedForAnimation
        val notExpandableChildItem = holder !is ExpandableChildViewHolder || holder.expandableItem == null
        if (notPreparedForAnimation || notExpandableChildItem) {
            dispatchRemoveFinished(holder)
            return true
        }

        val item = (holder as ExpandableChildViewHolder).expandableItem!!

        // Reset move state helps clear translationY when animation is being to be canceled
        if (pendingMoveAnimations.contains(holder)) {
            holder.itemView.animate().cancel()

            resetMoveState(holder)
        }

        if (pendingAddAnimations.contains(holder)) {
            holder.itemView.animate().cancel()
        } else {
            preRemoveImpl(holder)
        }

        if (item.groupId !in removeAnimations) removeAnimations[item.groupId] = mutableListOf()
        removeAnimations[item.groupId]?.add(holder)

        expandableAnimator.prepareAnimationToState(item.groupId, ExpandableAnimationItemState.Type.COLLAPSING)

        return true
    }

    override fun animateMove(holder: ViewHolder, fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
        val notPreparedForAnimation = !preparedForAnimation
        if (notPreparedForAnimation || holder !is ExpandableBaseViewHolder<*>) {
            dispatchMoveFinished(holder)
            return true
        }

        // Reset add state helps clear alpha and scale when animation is being to be canceled
        if (pendingAddAnimations.contains(holder)) {
            holder.itemView.animate().cancel()
            resetAddState(holder)
        }

        // Reset remove state helps clear alpha and scale when animation is being to be canceled
        if (pendingRemoveAnimations.contains(holder)) {
            holder.itemView.animate().cancel()
            resetRemoveState(holder)
        }

        if (pendingMoveAnimations.contains(holder)) {
            holder.itemView.animate().cancel()
        }

        preMoveImpl(holder, fromY, toY)
        moveAnimations.add(holder)
        return true
    }

    override fun animateChange(oldHolder: ViewHolder?, newHolder: ViewHolder?, fromLeft: Int, fromTop: Int, toLeft: Int, toTop: Int): Boolean {
        if (oldHolder == newHolder) {
            dispatchChangeFinished(newHolder, false)
        } else {
            dispatchChangeFinished(oldHolder, true)
            dispatchChangeFinished(newHolder, false)
        }
        return false
    }

    override fun runPendingAnimations() {
        // Add animation + expand items
        runExpandableAnimationFor(addAnimations, pendingAddAnimations) { animateAddImpl(it) }

        // Remove animation + collapse items
        runExpandableAnimationFor(removeAnimations, pendingRemoveAnimations) { animateRemoveImpl(it) }

        // Move animation
        val animatingViewHolders = moveAnimations.toList()
        moveAnimations.clear()

        for (holder in animatingViewHolders) {
            animateMoveImpl(holder)
        }

        pendingMoveAnimations.addAll(animatingViewHolders)

        // Set prepare for animation = false to return to skipping animations
        if (pendingAddAnimations.isNotEmpty() || pendingRemoveAnimations.isNotEmpty() || pendingMoveAnimations.isNotEmpty()) {
            preparedForAnimation = false
        }
    }

    private fun runExpandableAnimationFor(
        animationGroup: MutableMap<String, MutableList<ViewHolder>>,
        pendingAnimations: MutableSet<ViewHolder>,
        runAnimation: (ViewHolder) -> Unit
    ) {
        val parentItemIds = animationGroup.keys.toList()
        val animatingViewHolders = animationGroup.flatMap { (_, viewHolders) -> viewHolders }
        animationGroup.clear()

        parentItemIds.forEach { expandableAnimator.runAnimationFor(it) }
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

    abstract fun resetAddState(holder: ViewHolder)

    abstract fun resetRemoveState(holder: ViewHolder)

    abstract fun resetMoveState(holder: ViewHolder)

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
