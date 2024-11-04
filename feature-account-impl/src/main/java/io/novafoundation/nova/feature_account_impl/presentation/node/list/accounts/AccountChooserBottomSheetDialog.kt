package io.novafoundation.nova.feature_account_impl.presentation.node.list.accounts

import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ClickHandler
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.databinding.ItemAccountByNetworkBinding
import io.novafoundation.nova.feature_account_impl.presentation.node.list.accounts.model.AccountByNetworkModel

class AccountChooserBottomSheetDialog(
    context: Context,
    payload: Payload<AccountByNetworkModel>,
    onClicked: ClickHandler<AccountByNetworkModel>
) : DynamicListBottomSheet<AccountByNetworkModel>(context, payload, AccountDiffCallback, onClicked) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.profile_accounts_title)
    }

    override fun holderCreator(): HolderCreator<AccountByNetworkModel> = {
        AccountHolder(ItemAccountByNetworkBinding.inflate(it.inflater(), it, false))
    }
}

class AccountHolder(
    private val binder: ItemAccountByNetworkBinding
) : DynamicListSheetAdapter.Holder<AccountByNetworkModel>(binder.root) {

    override fun bind(item: AccountByNetworkModel, isSelected: Boolean, handler: DynamicListSheetAdapter.Handler<AccountByNetworkModel>) {
        super.bind(item, isSelected, handler)

        binder.accountTitle.text = item.name.orEmpty()
        binder.accountIcon.setImageDrawable(item.addressModel.image)
    }
}

private object AccountDiffCallback : DiffUtil.ItemCallback<AccountByNetworkModel>() {
    override fun areItemsTheSame(oldItem: AccountByNetworkModel, newItem: AccountByNetworkModel): Boolean {
        return oldItem.accountAddress == newItem.accountAddress
    }

    override fun areContentsTheSame(oldItem: AccountByNetworkModel, newItem: AccountByNetworkModel): Boolean {
        return oldItem == newItem
    }
}
