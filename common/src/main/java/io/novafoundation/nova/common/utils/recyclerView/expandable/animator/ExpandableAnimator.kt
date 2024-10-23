package io.novafoundation.nova.common.utils.recyclerView.expandable.animator

import android.animation.Animator
import android.animation.ValueAnimator
import androidx.core.animation.addListener
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.recyclerView.expandable.ExpandableAdapter
import io.novafoundation.nova.common.utils.recyclerView.expandable.ExpandableAnimationSettings
import io.novafoundation.nova.common.utils.recyclerView.expandable.flippedFraction
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableBaseItem
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableParentItem

/**
 * EXPANDING: animationFraction = 0f - fully collapsed and animationFraction = 1f - fully expanded
 * COLLAPSING: animationFraction = 0f - fully expanded and animationFraction = 1f - fully collapsed
 * So animationFraction is always move from 0f to 1f
 */
class ExpandableAnimationItemState(val animationType: Type, animationFraction: Float) {

    var animationFraction: Float = animationFraction
        internal set(value) {
            field = value.coerceIn(0f, 1f)
        }

    enum class Type {
        EXPANDING, COLLAPSING
    }
}

private class RunningAnimation(val currentState: ExpandableAnimationItemState, val animator: Animator)

class ExpandableAnimator(
    private val recyclerView: RecyclerView,
    private val animationSettings: ExpandableAnimationSettings,
    private val expandableAdapter: ExpandableAdapter
) {

    // It contains only items that is animating right now
    private val runningAnimations = mutableMapOf<String, RunningAnimation>()

    // Return current animation state for parent position or calculate state in [getExpandableItemState] if it isn't animating now
    fun getStateForPosition(position: Int): ExpandableAnimationItemState? {
        val items = expandableAdapter.getItems()
        val item = items.getOrNull(position) ?: return null
        if (item !is ExpandableParentItem) return null

        return runningAnimations[item.getId()]?.currentState ?: getExpandableItemState(position, items)
    }

    // Just prepare an animation without running
    fun prepareAnimationToState(parentId: String, type: ExpandableAnimationItemState.Type) {
        val existingSettings = runningAnimations[parentId]

        // No need to run animation if animation state is running and current type is the same
        if (existingSettings == null) {
            val state = ExpandableAnimationItemState(type, 0f)
            setAnimationFor(parentId, state)
        } else {
            // No need to update animation state if it's the same and already running
            if (existingSettings.currentState.animationType == type) {
                return
            }

            // Toggle animation state and flipping fraction to continue the animation but to another side
            val state = ExpandableAnimationItemState(type, existingSettings.currentState.flippedFraction())
            setAnimationFor(parentId, state)
        }
    }

    fun runAnimationFor(parentId: String) {
        val existingSettings = runningAnimations[parentId]
        existingSettings?.animator?.start()
    }

    private fun setAnimationFor(parentId: String, state: ExpandableAnimationItemState) {
        runningAnimations[parentId]?.animator?.cancel() // Cancel previous animation if it's exist

        val animator = ValueAnimator.ofFloat(state.animationFraction, 1f)
            .setDuration(animationSettings.duration)

        animator.interpolator = animationSettings.interpolator
        animator.addUpdateListener {
            state.animationFraction = it.animatedValue as Float
            recyclerView.invalidate()
        } // Invalidate recycler view to trigger onDraw in Item Decoration
        animator.addListener(onEnd = { runningAnimations.remove(parentId) })

        runningAnimations[parentId] = RunningAnimation(state, animator)
    }

    fun cancelAnimations() {
        runningAnimations.values
            .toList() // Copy list to avoid ConcurrentModificationException
            .forEach { it.animator.cancel() }
    }

    private fun getExpandableItemState(position: Int, items: List<ExpandableBaseItem>): ExpandableAnimationItemState {
        val nextItem = items.getOrNull(position + 1)

        // If next item is not a parent item it means current item is fully expanded
        return if (nextItem == null || nextItem is ExpandableParentItem) {
            ExpandableAnimationItemState(ExpandableAnimationItemState.Type.COLLAPSING, 1f)
        } else {
            ExpandableAnimationItemState(ExpandableAnimationItemState.Type.EXPANDING, 1f)
        }
    }
}
