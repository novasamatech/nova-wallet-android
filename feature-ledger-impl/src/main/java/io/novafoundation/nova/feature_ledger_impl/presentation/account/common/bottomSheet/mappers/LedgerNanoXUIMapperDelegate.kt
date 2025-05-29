package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.mappers

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand

class LedgerNanoXUIMapperDelegate(
    private val resourceManager: ResourceManager,
    private val device: LedgerDevice
) : LedgerDeviceMapperDelegate {

    override fun getName(): String {
        return device.name ?: resourceManager.getString(R.string.ledger_device_nano_x)
    }

    override fun getApproveImage(): LedgerMessageCommand.Graphics {
        return LedgerMessageCommand.Graphics(R.drawable.ic_ledger_nano_x_approve)
    }

    override fun getSignImage(): LedgerMessageCommand.Graphics {
        return getApproveImage()
    }

    override fun getErrorImage(): LedgerMessageCommand.Graphics {
        return LedgerMessageCommand.Graphics(R.drawable.ic_ledger_nano_x_error)
    }

    override fun getReviewAddressMessage(): String {
        return resourceManager.getString(R.string.ledger_verify_address_message_both_buttons, getName())
    }

    override fun getReviewAddressesMessage(): String {
        return resourceManager.getString(R.string.ledger_verify_addresses_message_both_buttons, getName())
    }

    override fun getSignMessage(): String {
        return resourceManager.getString(R.string.ledger_sign_approve_message, getName())
    }
}
