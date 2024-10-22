package io.novafoundation.nova.common.utils.recyclerView.expandable

import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimationItemState

fun ExpandableAnimationItemState.flippedFraction(): Float {
    return 1f - animationFraction
}

fun Float.flippedFraction(): Float {
    return 1f - this
}

fun ExpandableAnimationItemState.expandingFraction(): Float {
    return when (animationType) {
        ExpandableAnimationItemState.Type.EXPANDING -> animationFraction
        ExpandableAnimationItemState.Type.COLLAPSING -> flippedFraction()
    }
}
