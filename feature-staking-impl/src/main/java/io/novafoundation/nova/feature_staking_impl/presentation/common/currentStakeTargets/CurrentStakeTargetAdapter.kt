package io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets

import android.view.ViewGroup
import io.novafoundation.nova.common.list.BaseGroupedDiffCallback
import io.novafoundation.nova.common.list.GroupedListAdapter
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.feature_staking_impl.databinding.ItemCurrentValidatorBinding
import io.novafoundation.nova.feature_staking_impl.databinding.ItemCurrentValidatorGroupBinding
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.model.SelectedStakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.common.currentStakeTargets.model.SelectedStakeTargetStatusModel

class CurrentStakeTargetAdapter(
    private val handler: Handler,
) : GroupedListAdapter<SelectedStakeTargetStatusModel, SelectedStakeTargetModel>(CurrentValidatorsDiffCallback) {

    interface Handler {

        fun infoClicked(stakeTargetModel: SelectedStakeTargetModel)
    }

    override fun createGroupViewHolder(parent: ViewGroup): GroupedListHolder {
        return CurrentValidatorsGroupHolder(ItemCurrentValidatorGroupBinding.inflate(parent.inflater(), parent, false))
    }

    override fun createChildViewHolder(parent: ViewGroup): GroupedListHolder {
        return CurrentValidatorsChildHolder(ItemCurrentValidatorBinding.inflate(parent.inflater(), parent, false))
    }

    override fun bindGroup(holder: GroupedListHolder, group: SelectedStakeTargetStatusModel) {
        (holder as CurrentValidatorsGroupHolder).bind(group)
    }

    override fun bindChild(holder: GroupedListHolder, child: SelectedStakeTargetModel) {
        (holder as CurrentValidatorsChildHolder).bind(child, handler)
    }
}

private class CurrentValidatorsGroupHolder(private val binder: ItemCurrentValidatorGroupBinding) : GroupedListHolder(binder.root) {

    fun bind(group: SelectedStakeTargetStatusModel) = with(binder) {
        val topPadding = if (isFirst()) 16 else 24
        itemCurrentValidatorContainer.updatePadding(top = topPadding.dp(binder.root.context))

        itemCurrentValidatorGroupStatus.setTextOrHide(group.titleConfig?.text)

        group.titleConfig?.let {
            itemCurrentValidatorGroupStatus.setTextColorRes(it.textColorRes)
            itemCurrentValidatorGroupStatus.setDrawableStart(it.iconRes, widthInDp = 16, paddingInDp = 8, tint = it.iconTintRes)
        }

        itemCurrentValidatorGroupDescription.text = group.description
    }

    private fun isFirst() = absoluteAdapterPosition == 0
}

private class CurrentValidatorsChildHolder(private val binder: ItemCurrentValidatorBinding) : GroupedListHolder(binder.root) {

    fun bind(validator: SelectedStakeTargetModel, handler: CurrentStakeTargetAdapter.Handler) = with(binder) {
        itemCurrentValidatorIcon.setImageDrawable(validator.addressModel.image)
        itemCurrentValidatorName.text = validator.addressModel.nameOrAddress

        itemCurrentValidatorNominated.setVisible(validator.nominated != null)
        itemCurrentValidatorNominatedAmount.text = validator.nominated?.token

        itemCurrentValidatorApy.setTextOrHide(validator.apy)

        itemCurrentValidatorInfo.setOnClickListener { handler.infoClicked(validator) }

        itemCurrentValidatorOversubscribed.setVisible(validator.isOversubscribed)
        currentValidatorSlashedIcon.setVisible(validator.isSlashed)
    }
}

private object CurrentValidatorsDiffCallback :
    BaseGroupedDiffCallback<SelectedStakeTargetStatusModel, SelectedStakeTargetModel>(SelectedStakeTargetStatusModel::class.java) {

    override fun areGroupItemsTheSame(oldItem: SelectedStakeTargetStatusModel, newItem: SelectedStakeTargetStatusModel): Boolean {
        return oldItem == newItem
    }

    override fun areGroupContentsTheSame(oldItem: SelectedStakeTargetStatusModel, newItem: SelectedStakeTargetStatusModel): Boolean {
        return true
    }

    override fun areChildItemsTheSame(oldItem: SelectedStakeTargetModel, newItem: SelectedStakeTargetModel): Boolean {
        return oldItem.addressModel.address == newItem.addressModel.address
    }

    override fun areChildContentsTheSame(oldItem: SelectedStakeTargetModel, newItem: SelectedStakeTargetModel): Boolean {
        return oldItem.nominated == newItem.nominated
    }
}
