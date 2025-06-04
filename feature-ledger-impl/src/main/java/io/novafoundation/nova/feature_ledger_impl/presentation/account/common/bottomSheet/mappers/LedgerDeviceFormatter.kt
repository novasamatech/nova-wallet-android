package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.mappers

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDeviceType
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand

class LedgerDeviceFormatter(private val resourceManager: ResourceManager) {

    fun formatName(device: LedgerDevice): String = createDelegate(device).getName()

    fun createDelegate(device: LedgerDevice): LedgerDeviceMapperDelegate {
        return when (device.deviceType) {
            LedgerDeviceType.STAX -> LedgerStaxMapperDelegate(resourceManager, device)
            LedgerDeviceType.FLEX -> LedgerFlexMapperDelegate(resourceManager, device)
            LedgerDeviceType.NANO_X -> LedgerNanoXUIMapperDelegate(resourceManager, device)
            LedgerDeviceType.NANO_S_PLUS -> LedgerNanoSPlusMapperDelegate(resourceManager, device)
            LedgerDeviceType.NANO_S -> LedgerNanoSMapperDelegate(resourceManager, device)
        }
    }
}

interface LedgerDeviceMapperDelegate {

    fun getName(): String

    fun getApproveImage(): LedgerMessageCommand.Graphics

    fun getErrorImage(): LedgerMessageCommand.Graphics

    fun getSignImage(): LedgerMessageCommand.Graphics

    fun getReviewAddressMessage(): String

    fun getReviewAddressesMessage(): String

    fun getSignMessage(): String
}
