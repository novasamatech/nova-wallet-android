package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.view

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.utils.addAfter
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.AccentActionView
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ClickHandler
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.model.SelectCollatorModel
import kotlinx.android.synthetic.main.item_select_staked_collator.view.itemSelectStakedCollatorCheck
import kotlinx.android.synthetic.main.item_select_staked_collator.view.itemSelectStakedCollatorCollator
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorInfo
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorName
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorSubtitleLabel
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorSubtitleValue

private val INACTIVE_COLOR = R.color.white_30

class ChooseStakedCollatorBottomSheet(
    context: Context,
    payload: Payload<SelectCollatorModel>,
    stakedCollatorSelected: ClickHandler<SelectCollatorModel>,
    onCancel: () -> Unit,
    private val newCollatorClicked: ClickHandler<Unit>?
) : DynamicListBottomSheet<SelectCollatorModel>(
    context = context,
    payload = payload,
    diffCallback = DiffCallback(),
    onClicked = stakedCollatorSelected,
    onCancel = onCancel
) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.staking_parachain_collator)
        setDividerVisible(false)

        maybeAddNewCollatorButton()
    }

    override fun holderCreator(): HolderCreator<SelectCollatorModel> = { parentViewGroup ->
        ViewHolder(parentViewGroup.inflateChild(R.layout.item_select_staked_collator))
    }

    private fun maybeAddNewCollatorButton() {
        if (newCollatorClicked != null) {
            val newCollatorAction = AccentActionView(context).apply {
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

                setDismissingClickListener { newCollatorClicked.invoke(Unit) }

                setText(R.string.staking_parachain_new_collator)
                setIcon(R.drawable.ic_add_circle)
            }

            container.addAfter(titleView, newCollatorAction)
        }
    }
}

private class ViewHolder(containerView: View) : DynamicListSheetAdapter.Holder<SelectCollatorModel>(containerView) {

    init {
        containerView.itemSelectStakedCollatorCollator.itemValidatorInfo.makeGone()
        containerView.itemSelectStakedCollatorCollator.background = null
    }

    override fun bind(item: SelectCollatorModel, isSelected: Boolean, handler: DynamicListSheetAdapter.Handler<SelectCollatorModel>) = with(containerView) {
        super.bind(item, isSelected, handler)

        itemSelectStakedCollatorCollator.bindSelectedCollator(item)
        itemSelectStakedCollatorCheck.isChecked = isSelected

        val primaryTextColor = if (item.active) R.color.white else INACTIVE_COLOR
        val secondaryTextColor = if (item.active) R.color.white_64 else INACTIVE_COLOR

        with(itemSelectStakedCollatorCollator) {
            itemValidatorName.setTextColorRes(primaryTextColor)
            itemValidatorSubtitleLabel.setTextColorRes(secondaryTextColor)
            itemValidatorSubtitleValue.setTextColorRes(primaryTextColor)
        }
    }
}

private class DiffCallback : DiffUtil.ItemCallback<SelectCollatorModel>() {
    override fun areContentsTheSame(oldItem: SelectCollatorModel, newItem: SelectCollatorModel): Boolean {
        return oldItem.staked == newItem.staked && oldItem.active != newItem.active
    }

    override fun areItemsTheSame(oldItem: SelectCollatorModel, newItem: SelectCollatorModel): Boolean {
        return oldItem.addressModel.address == newItem.addressModel.address
    }
}
