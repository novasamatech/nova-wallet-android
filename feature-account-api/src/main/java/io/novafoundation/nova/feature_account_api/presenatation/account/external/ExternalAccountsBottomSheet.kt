package io.novafoundation.nova.feature_account_api.presenatation.account.external

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ClickHandler
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.databinding.ItemExternalAccountIdentifierBinding
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputState
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.externalAccount.ExternalAccount

class ExternalAccountsBottomSheet(
    context: Context,
    private val title: String,
    payload: Payload<ExternalAccount>,
    onClicked: ClickHandler<ExternalAccount>
) : DynamicListBottomSheet<ExternalAccount>(context, payload, AccountDiffCallback, onClicked, dismissOnClick = false) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(title)
    }

    override fun holderCreator(): HolderCreator<ExternalAccount> = {
        AccountHolder(ItemExternalAccountIdentifierBinding.inflate(it.inflater(), it, false))
    }
}

class AccountHolder(
    private val binder: ItemExternalAccountIdentifierBinding
) : DynamicListSheetAdapter.Holder<ExternalAccount>(binder.root) {

    override fun bind(item: ExternalAccount, isSelected: Boolean, handler: DynamicListSheetAdapter.Handler<ExternalAccount>) {
        super.bind(item, isSelected, handler)

        with(itemView) {
            setIdenticonState(item.icon)
            binder.externalAccountIdentifierTitle.text = item.description ?: item.address
            binder.externalAccountIdentifierSubtitle.isVisible = item.description != null
            binder.externalAccountIdentifierSubtitle.text = item.address
            binder.externalAccountIdentifierIsSelected.isChecked = isSelected

            if (item.description == null) {
                binder.externalAccountIdentifierTitle.ellipsize = TextUtils.TruncateAt.MIDDLE
            } else {
                binder.externalAccountIdentifierTitle.ellipsize = TextUtils.TruncateAt.END
            }
        }
    }

    private fun setIdenticonState(state: AddressInputState.IdenticonState) = when (state) {
        is AddressInputState.IdenticonState.Address -> {
            binder.externalAccountIcon.setImageDrawable(state.drawable)
        }

        AddressInputState.IdenticonState.Placeholder -> {
            binder.externalAccountIcon.setImageResource(R.drawable.ic_identicon_placeholder)
        }
    }
}

private object AccountDiffCallback : DiffUtil.ItemCallback<ExternalAccount>() {
    override fun areItemsTheSame(oldItem: ExternalAccount, newItem: ExternalAccount): Boolean {
        return oldItem.address == newItem.address
    }

    override fun areContentsTheSame(oldItem: ExternalAccount, newItem: ExternalAccount): Boolean {
        return false
    }
}
