package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position

import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ClickHandler
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.feature_account_api.databinding.ItemAccountChooserBinding
import io.novafoundation.nova.feature_staking_impl.R

/** iOS UNST-21 validator picker: identicon, name or shortened address, "Staked: X", radio. */
class BittensorPositionChoiceBottomSheet(
    context: Context,
    choices: List<BittensorPositionChoiceUi>,
    onClicked: ClickHandler<BittensorPositionChoiceUi>
) : DynamicListBottomSheet<BittensorPositionChoiceUi>(
    context = context,
    payload = Payload(choices),
    diffCallback = ChoiceDiff,
    onClicked = onClicked
) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.bittensor_validator)
    }

    override fun holderCreator(): HolderCreator<BittensorPositionChoiceUi> = { parent ->
        ChoiceHolder(ItemAccountChooserBinding.inflate(parent.inflater(), parent, false))
    }
}

private class ChoiceHolder(private val binder: ItemAccountChooserBinding) : DynamicListSheetAdapter.Holder<BittensorPositionChoiceUi>(binder.root) {

    override fun bind(item: BittensorPositionChoiceUi, isSelected: Boolean, handler: DynamicListSheetAdapter.Handler<BittensorPositionChoiceUi>) {
        super.bind(item, isSelected, handler)

        with(binder.itemAccountChooserAddress) {
            setIcon(item.icon)
            setTitle(item.choice.title)
            setSubTitle(context.getString(R.string.bittensor_position_choice_staked, item.choice.staked))
        }
        binder.itemAccountChooserCheck.isChecked = isSelected
    }
}

private object ChoiceDiff : DiffUtil.ItemCallback<BittensorPositionChoiceUi>() {

    override fun areItemsTheSame(oldItem: BittensorPositionChoiceUi, newItem: BittensorPositionChoiceUi) = oldItem.choice.hotkey == newItem.choice.hotkey

    override fun areContentsTheSame(oldItem: BittensorPositionChoiceUi, newItem: BittensorPositionChoiceUi) = oldItem.choice == newItem.choice
}
