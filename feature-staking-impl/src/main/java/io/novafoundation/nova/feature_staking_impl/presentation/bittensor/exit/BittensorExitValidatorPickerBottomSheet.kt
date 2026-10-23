package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.common.utils.addAfter
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.AccentActionView
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ItemSelectStakedCollatorBinding
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.SelectStakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.view.bindSelectedCollator
import io.novasama.substrate_sdk_android.extensions.toHexString

/** One row of the validator picker; the picker only exists for a group of more than one validator. */
data class BittensorExitValidatorChoice(val hotkey: AccountIdKey) : Identifiable {

    override val identifier: String = hotkey.value.toHexString()
}

class BittensorExitValidatorPicker(
    val rows: List<SelectStakeTargetModel<BittensorExitValidatorChoice>>,
    val selected: SelectStakeTargetModel<BittensorExitValidatorChoice>?,
    val allValidators: Boolean
)

private typealias ValidatorRow = SelectStakeTargetModel<BittensorExitValidatorChoice>

/** iOS `SubtensorValidatorPickerViewModelFactory`: the group's validators with their stake, "All validators" on top. */
class BittensorExitValidatorPickerBottomSheet(
    context: Context,
    picker: BittensorExitValidatorPicker,
    onValidatorPicked: (BittensorExitValidatorChoice) -> Unit,
    private val onAllValidatorsPicked: (() -> Unit)?
) : DynamicListBottomSheet<ValidatorRow>(
    context = context,
    payload = DynamicListBottomSheet.Payload(picker.rows, picker.selected),
    diffCallback = DiffCallback(),
    onClicked = { _, row -> onValidatorPicked(row.payload) }
) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.bittensor_validator)

        onAllValidatorsPicked?.let { onPicked ->
            val allValidators = AccentActionView(context).apply {
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

                setDismissingClickListener { onPicked() }

                setText(R.string.bittensor_exit_all_validators)
            }

            container.addAfter(headerView, allValidators)
        }
    }

    override fun holderCreator(): HolderCreator<ValidatorRow> = { parent ->
        ValidatorHolder(ItemSelectStakedCollatorBinding.inflate(parent.inflater(), parent, false))
    }
}

private class ValidatorHolder(
    private val binder: ItemSelectStakedCollatorBinding
) : DynamicListSheetAdapter.Holder<ValidatorRow>(binder.root) {

    init {
        with(binder) {
            itemSelectStakedCollatorCollator.root.background = null
            itemSelectStakedCollatorCollator.itemStakingTargetSubtitleLabel.makeGone()
            itemSelectStakedCollatorCollator.itemStakingTargetInfo.makeGone()
            itemSelectStakedCollatorCheck.makeVisible()
        }
    }

    override fun bind(item: ValidatorRow, isSelected: Boolean, handler: DynamicListSheetAdapter.Handler<ValidatorRow>) = with(binder) {
        super.bind(item, isSelected, handler)

        itemSelectStakedCollatorCollator.bindSelectedCollator(item)
        itemSelectStakedCollatorCheck.isChecked = isSelected

        val primaryTextColor = if (item.active) R.color.text_primary else R.color.text_secondary

        with(itemSelectStakedCollatorCollator) {
            itemStakingTargetName.setTextColorRes(primaryTextColor)
            itemStakingTargetSubtitleValue.setTextColorRes(primaryTextColor)
        }
    }
}

private class DiffCallback : DiffUtil.ItemCallback<ValidatorRow>() {

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: ValidatorRow, newItem: ValidatorRow): Boolean {
        return oldItem.subtitle.toString() == newItem.subtitle.toString() && oldItem.active == newItem.active
    }

    override fun areItemsTheSame(oldItem: ValidatorRow, newItem: ValidatorRow): Boolean {
        return oldItem.payload == newItem.payload
    }
}
