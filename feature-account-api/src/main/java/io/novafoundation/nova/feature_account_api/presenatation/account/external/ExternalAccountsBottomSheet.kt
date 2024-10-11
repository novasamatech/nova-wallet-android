package io.novafoundation.nova.feature_account_api.presenatation.account.external

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ClickHandler
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.feature_account_api.R
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
        AccountHolder(it.inflateChild(R.layout.item_external_account_identifier))
    }
}

class AccountHolder(
    itemView: View
) : DynamicListSheetAdapter.Holder<ExternalAccount>(itemView) {

    override fun bind(item: ExternalAccount, isSelected: Boolean, handler: DynamicListSheetAdapter.Handler<ExternalAccount>) {
        super.bind(item, isSelected, handler)

        with(itemView) {
            setIdenticonState(item.icon)
            externalAccountIdentifierTitle.text = item.description ?: item.address
            externalAccountIdentifierSubtitle.isVisible = item.description != null
            externalAccountIdentifierSubtitle.text = item.address
            externalAccountIdentifierIsSelected.isChecked = isSelected

            if (item.description == null) {
                externalAccountIdentifierTitle.ellipsize = TextUtils.TruncateAt.MIDDLE
            } else {
                externalAccountIdentifierTitle.ellipsize = TextUtils.TruncateAt.END
            }
        }
    }

    private fun setIdenticonState(state: AddressInputState.IdenticonState) = with(itemView) {
        when (state) {
            is AddressInputState.IdenticonState.Address -> {
                externalAccountIcon.setImageDrawable(state.drawable)
            }
            AddressInputState.IdenticonState.Placeholder -> {
                externalAccountIcon.setImageResource(R.drawable.ic_identicon_placeholder)
            }
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
