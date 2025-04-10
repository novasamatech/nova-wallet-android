package io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.annotation.StringRes
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.common.utils.addAfter
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.AccentActionView
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ClickHandler
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ItemSelectStakedCollatorBinding
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.ChooseStakedStakeTargetsBottomSheet.SelectionStyle
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.view.bindSelectedCollator

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

    override fun holderCreator(): HolderCreator<SelectStakeTargetModel<T>> = { parent ->
        ViewHolder(
            binder = ItemSelectStakedCollatorBinding.inflate(parent.inflater(), parent, false),
            selectionStyle = selectionStyle
        )
    }

    private fun maybeAddNewCollatorButton() {
        if (newStakeTargetClicked != null) {
            val newCollatorAction = AccentActionView(context).apply {
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

                setDismissingClickListener { newStakeTargetClicked.invoke(this@ChooseStakedStakeTargetsBottomSheet, Unit) }

                setText(R.string.staking_parachain_new_collator)
                setIcon(R.drawable.ic_add_circle)
            }

            container.addAfter(headerView, newCollatorAction)
        }
    }
}

private class ViewHolder<T : Identifiable>(
    private val binder: ItemSelectStakedCollatorBinding,
    private val selectionStyle: SelectionStyle
) : DynamicListSheetAdapter.Holder<SelectStakeTargetModel<T>>(binder.root) {

    init {
        setInitialState()
    }

    override fun bind(
        item: SelectStakeTargetModel<T>,
        isSelected: Boolean,
        handler: DynamicListSheetAdapter.Handler<SelectStakeTargetModel<T>>
    ) = with(binder) {
        super.bind(item, isSelected, handler)

        itemSelectStakedCollatorCollator.bindSelectedCollator(item)
        itemSelectStakedCollatorCheck.isChecked = isSelected

        val primaryTextColor = if (item.active) R.color.text_primary else R.color.text_secondary

        with(itemSelectStakedCollatorCollator) {
            itemStakingTargetName.setTextColorRes(primaryTextColor)
            itemStakingTargetSubtitleValue.setTextColorRes(primaryTextColor)
        }
    }

    private fun setInitialState() = with(binder) {
        itemSelectStakedCollatorCollator.root.background = null
        itemSelectStakedCollatorCollator.itemStakingTargetSubtitleLabel.makeGone()

        when (selectionStyle) {
            SelectionStyle.RadioGroup -> {
                itemSelectStakedCollatorCollator.itemStakingTargetInfo.makeGone()
                itemSelectStakedCollatorCheck.makeVisible()
            }
            SelectionStyle.Arrow -> {
                itemSelectStakedCollatorCheck.makeGone()
                itemSelectStakedCollatorCollator.itemStakingTargetInfo.makeVisible()
                itemSelectStakedCollatorCollator.itemStakingTargetInfo.setImageResource(R.drawable.ic_chevron_right)
            }
        }
    }
}

fun <T : Identifiable> ChooseStakedStakeTargetsBottomSheet(
    context: Context,
    payload: ChooseStakedStakeTargetsBottomSheet.Payload<SelectStakeTargetModel<T>>,
    onResponse: (ChooseStakedStakeTargetsResponse<T>) -> Unit,
    onCancel: () -> Unit,
    selectionStyle: SelectionStyle = SelectionStyle.RadioGroup
): ChooseStakedStakeTargetsBottomSheet<T> {
    return ChooseStakedStakeTargetsBottomSheet(
        context = context,
        payload = payload,
        stakedCollatorSelected = { _, targetModel -> onResponse(ChooseStakedStakeTargetsResponse.Existing(targetModel.payload)) },
        onCancel = onCancel,
        newStakeTargetClicked = { _, _ -> onResponse(ChooseStakedStakeTargetsResponse.New) },
        selectionStyle = selectionStyle
    )
}

private class DiffCallback<T : Identifiable> : DiffUtil.ItemCallback<SelectStakeTargetModel<T>>() {

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: SelectStakeTargetModel<T>, newItem: SelectStakeTargetModel<T>): Boolean {
        return oldItem.subtitle.toString() == newItem.subtitle.toString() && oldItem.active != newItem.active
    }

    override fun areItemsTheSame(oldItem: SelectStakeTargetModel<T>, newItem: SelectStakeTargetModel<T>): Boolean {
        return oldItem.addressModel.address == newItem.addressModel.address
    }
}
