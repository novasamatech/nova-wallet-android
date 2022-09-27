package io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.annotation.StringRes
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.common.utils.addAfter
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.AccentActionView
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ClickHandler
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.ChooseStakedStakeTargetsBottomSheet.SelectionStyle
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.view.bindSelectedCollator
import kotlinx.android.synthetic.main.item_select_staked_collator.view.itemSelectStakedCollatorCheck
import kotlinx.android.synthetic.main.item_select_staked_collator.view.itemSelectStakedCollatorCollator
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorInfo
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorName
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorSubtitleLabel
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorSubtitleValue

class ChooseStakedStakeTargetsBottomSheet<T : Identifiable>(
    context: Context,
    private val payload: Payload<SelectStakeTargetModel<T>>,
    stakedCollatorSelected: ClickHandler<SelectStakeTargetModel<T>>,
    onCancel: () -> Unit,
    private val newStakeTargetClicked: ClickHandler<Unit>?,
    private val selectionStyle: SelectionStyle = SelectionStyle.RadioGroup
) : DynamicListBottomSheet<SelectStakeTargetModel<T>>(
    context = context,
    payload = payload,
    diffCallback = DiffCallback(),
    onClicked = stakedCollatorSelected,
    onCancel = onCancel
) {

    class Payload<out T>(
        data: List<T>,
        selected: T? = null,
        @StringRes val titleRes: Int = R.string.staking_parachain_collator,
    ) : DynamicListBottomSheet.Payload<T>(data, selected)

    enum class SelectionStyle {
        RadioGroup, Arrow
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(payload.titleRes)

        maybeAddNewCollatorButton()
    }

    override fun holderCreator(): HolderCreator<SelectStakeTargetModel<T>> = { parentViewGroup ->
        ViewHolder(
            containerView = parentViewGroup.inflateChild(R.layout.item_select_staked_collator),
            selectionStyle = selectionStyle
        )
    }

    private fun maybeAddNewCollatorButton() {
        if (newStakeTargetClicked != null) {
            val newCollatorAction = AccentActionView(context).apply {
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

                setDismissingClickListener { newStakeTargetClicked.invoke(Unit) }

                setText(R.string.staking_parachain_new_collator)
                setIcon(R.drawable.ic_add_circle)
            }

            container.addAfter(headerView, newCollatorAction)
        }
    }
}

private class ViewHolder<T : Identifiable>(
    containerView: View,
    private val selectionStyle: SelectionStyle
) : DynamicListSheetAdapter.Holder<SelectStakeTargetModel<T>>(containerView) {

    init {
        setInitialState()
    }

    override fun bind(
        item: SelectStakeTargetModel<T>,
        isSelected: Boolean,
        handler: DynamicListSheetAdapter.Handler<SelectStakeTargetModel<T>>
    ) = with(containerView) {
        super.bind(item, isSelected, handler)

        itemSelectStakedCollatorCollator.bindSelectedCollator(item)
        itemSelectStakedCollatorCheck.isChecked = isSelected

        val primaryTextColor = if (item.active) R.color.white else R.color.textInactive

        with(itemSelectStakedCollatorCollator) {
            itemValidatorName.setTextColorRes(primaryTextColor)
            itemValidatorSubtitleValue.setTextColorRes(primaryTextColor)
        }
    }

    private fun setInitialState() = with(containerView) {
        itemSelectStakedCollatorCollator.background = null
        itemSelectStakedCollatorCollator.itemValidatorSubtitleLabel.makeGone()

        when (selectionStyle) {
            SelectionStyle.RadioGroup -> {
                itemSelectStakedCollatorCollator.itemValidatorInfo.makeGone()
                itemSelectStakedCollatorCheck.makeVisible()
            }
            SelectionStyle.Arrow -> {
                itemSelectStakedCollatorCheck.makeGone()
                itemSelectStakedCollatorCollator.itemValidatorInfo.makeVisible()
                itemSelectStakedCollatorCollator.itemValidatorInfo.setImageResource(R.drawable.ic_chevron_right)
            }
        }
    }
}

private class DiffCallback<T : Identifiable> : DiffUtil.ItemCallback<SelectStakeTargetModel<T>>() {

    override fun areContentsTheSame(oldItem: SelectStakeTargetModel<T>, newItem: SelectStakeTargetModel<T>): Boolean {
        return oldItem.subtitle == newItem.subtitle && oldItem.active != newItem.active
    }

    override fun areItemsTheSame(oldItem: SelectStakeTargetModel<T>, newItem: SelectStakeTargetModel<T>): Boolean {
        return oldItem.addressModel.address == newItem.addressModel.address
    }
}
