package io.novafoundation.nova.feature_account_api.presenatation.account.chooser

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ClickHandler
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.feature_account_api.R
import kotlinx.android.synthetic.main.item_account_chooser.view.itemAccountChooserAddress
import kotlinx.android.synthetic.main.item_account_chooser.view.itemAccountChooserCheck

class AccountChooserBottomSheetDialog(
    context: Context,
    payload: Payload<AddressModel>,
    clickHandler: ClickHandler<AddressModel>,
    @StringRes val title: Int
) : DynamicListBottomSheet<AddressModel>(
    context, payload, AddressModelDiffCallback, clickHandler
) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(title)
        setDividerVisible(false)
    }

    override fun holderCreator(): HolderCreator<AddressModel> = { parent ->
        AddressModelHolder(parent.inflateChild(R.layout.item_account_chooser))
    }
}

private class AddressModelHolder(parent: View) : DynamicListSheetAdapter.Holder<AddressModel>(parent) {

    override fun bind(
        item: AddressModel,
        isSelected: Boolean,
        handler: DynamicListSheetAdapter.Handler<AddressModel>
    ) {
        super.bind(item, isSelected, handler)

        with(itemView) {
            itemAccountChooserAddress.setAddressModel(item)
            itemAccountChooserCheck.isChecked = isSelected
        }
    }
}

private object AddressModelDiffCallback : DiffUtil.ItemCallback<AddressModel>() {
    override fun areItemsTheSame(oldItem: AddressModel, newItem: AddressModel): Boolean {
        return oldItem.address == newItem.address
    }

    override fun areContentsTheSame(oldItem: AddressModel, newItem: AddressModel): Boolean {
        return true
    }
}
