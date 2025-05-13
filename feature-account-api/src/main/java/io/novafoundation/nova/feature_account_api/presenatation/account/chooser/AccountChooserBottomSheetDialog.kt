package io.novafoundation.nova.feature_account_api.presenatation.account.chooser

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ClickHandler
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.feature_account_api.databinding.ItemAccountChooserBinding

class AccountChooserBottomSheetDialog(
    context: Context,
    payload: Payload<AddressModel>,
    onSuccess: ClickHandler<AddressModel>,
    onCancel: (() -> Unit)? = null,
    @StringRes val title: Int
) : DynamicListBottomSheet<AddressModel>(
    context = context,
    payload = payload,
    diffCallback = AddressModelDiffCallback,
    onClicked = onSuccess,
    onCancel = onCancel
) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(title)
    }

    override fun holderCreator(): HolderCreator<AddressModel> = { parent ->
        AddressModelHolder(ItemAccountChooserBinding.inflate(parent.inflater(), parent, false))
    }
}

private class AddressModelHolder(private val binder: ItemAccountChooserBinding) : DynamicListSheetAdapter.Holder<AddressModel>(binder.root) {

    override fun bind(
        item: AddressModel,
        isSelected: Boolean,
        handler: DynamicListSheetAdapter.Handler<AddressModel>
    ) {
        super.bind(item, isSelected, handler)

        with(binder) {
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
