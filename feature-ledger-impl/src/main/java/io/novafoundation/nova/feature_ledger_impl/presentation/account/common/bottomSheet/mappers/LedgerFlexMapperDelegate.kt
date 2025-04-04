package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.mappers

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand

class LedgerFlexMapperDelegate(
    private val resourceManager: ResourceManager,
    private val device: LedgerDevice
) : LedgerDeviceMapperDelegate {

    override fun getName(): String {
        return device.name ?: resourceManager.getString(R.string.ledger_device_flex)
    }

    override fun getApproveImage(): LedgerMessageCommand.Graphics {
        return LedgerMessageCommand.Graphics(R.drawable.ic_ledger_flex_approve)
    }

    override fun getSignImage(): LedgerMessageCommand.Graphics {
        return LedgerMessageCommand.Graphics(R.drawable.ic_ledger_flex_sign)
    }

    override fun getErrorImage(): LedgerMessageCommand.Graphics {
        return LedgerMessageCommand.Graphics(R.drawable.ic_ledger_flex_error)
    }

    override fun getReviewAddressMessage(): String {
        return resourceManager.getString(R.string.ledger_verify_address_message_confirm_button, getName())
    }

    override fun getSignMessage(): String {
        return resourceManager.getString(R.string.ledger_hold_to_sign, getName())
    }
}
