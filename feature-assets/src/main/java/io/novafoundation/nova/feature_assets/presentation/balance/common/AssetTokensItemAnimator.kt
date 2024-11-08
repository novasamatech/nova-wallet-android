package io.novafoundation.nova.feature_assets.presentation.balance.common

import android.view.ViewPropertyAnimator
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.recyclerView.expandable.ExpandableAnimationSettings
import io.novafoundation.nova.common.utils.recyclerView.expandable.ExpandableItemAnimator
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimator

private const val REMOVE_SCALE = 0.9f

class AssetTokensItemAnimator(
    settings: ExpandableAnimationSettings,
    expandableAnimator: ExpandableAnimator
) : ExpandableItemAnimator(
    settings,
    expandableAnimator
) {

    override fun preAddImpl(holder: RecyclerView.ViewHolder) {
        holder.itemView.alpha = 0f
        holder.itemView.scaleX = REMOVE_SCALE
        holder.itemView.scaleY = REMOVE_SCALE
    }

    override fun getAddAnimator(holder: RecyclerView.ViewHolder): ViewPropertyAnimator {
        return holder.itemView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
    }

    override fun preRemoveImpl(holder: RecyclerView.ViewHolder) {
        resetAddState(holder)
    }

    override fun getRemoveAnimator(holder: RecyclerView.ViewHolder): ViewPropertyAnimator {
        return holder.itemView.animate()
            .alpha(0f)
            .scaleX(REMOVE_SCALE)
            .scaleY(REMOVE_SCALE)
    }

    override fun preMoveImpl(holder: RecyclerView.ViewHolder, fromY: Int, toY: Int) {
        val yDelta = toY - fromY
        holder.itemView.translationY += -yDelta
    }

    override fun getMoveAnimator(holder: RecyclerView.ViewHolder): ViewPropertyAnimator {
        return holder.itemView.animate()
            .translationY(0f)
    }

    override fun endAnimation(viewHolder: RecyclerView.ViewHolder) {
        super.endAnimation(viewHolder)

        viewHolder.itemView.translationY = 0f
        viewHolder.itemView.alpha = 1f
        viewHolder.itemView.scaleX = 1f
        viewHolder.itemView.scaleY = 1f
    }

    override fun resetAddState(holder: RecyclerView.ViewHolder) {
        holder.itemView.alpha = 1f
        holder.itemView.scaleX = 1f
        holder.itemView.scaleY = 1f
    }

    override fun resetRemoveState(holder: RecyclerView.ViewHolder) {
        holder.itemView.alpha = 1f
        holder.itemView.scaleX = 1f
        holder.itemView.scaleY = 1f
    }

    override fun resetMoveState(holder: RecyclerView.ViewHolder) {
        holder.itemView.translationY = 0f
    }
}
