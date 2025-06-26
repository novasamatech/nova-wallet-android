package io.novafoundation.nova.feature_account_api.presenatation.addressActions

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.textItem
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.databinding.BottomSheetAddressActionsBinding

typealias CopyAddressCallback = (AddressActionsMixin.Payload) -> Unit

class AddressActionsSheet(
    context: Context,
    private val payload: AddressActionsMixin.Payload,
    val onCopy: CopyAddressCallback,
) : FixedListBottomSheet<BottomSheetAddressActionsBinding>(
    context,
    viewConfiguration = ViewConfiguration(
        configurationBinder = BottomSheetAddressActionsBinding.inflate(LayoutInflater.from(context)),
        title = { configurationBinder.addressActionsValue },
        container = { configurationBinder.addressActionsContainer }
    )
) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binder.addressActionsScheme.setModel(payload.addressTypeLabel)
        binder.addressActionsIcon.setImageDrawable(payload.addressModel.image)

        binder.addressActionsValue.text = payload.addressModel.address

        textItem(R.drawable.ic_copy_outline, R.string.common_copy_address) {
            onCopy(payload)
        }
    }
}
