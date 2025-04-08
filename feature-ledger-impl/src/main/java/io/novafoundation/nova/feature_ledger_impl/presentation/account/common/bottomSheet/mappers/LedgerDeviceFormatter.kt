package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.mappers

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDeviceType
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand

class LedgerDeviceFormatter(private val resourceManager: ResourceManager) {

    fun mapName(device: LedgerDevice): String = createDelegate(device).getName()

    fun mapApproveImage(device: LedgerDevice): LedgerMessageCommand.Graphics = createDelegate(device).getApproveImage()

    fun mapErrorImage(device: LedgerDevice): LedgerMessageCommand.Graphics = createDelegate(device).getErrorImage()

    fun mapSignImage(device: LedgerDevice): LedgerMessageCommand.Graphics = createDelegate(device).getSignImage()

    fun mapReviewAddressMessage(device: LedgerDevice): String = createDelegate(device).getReviewAddressMessage()

    fun mapSignMessage(device: LedgerDevice): String = createDelegate(device).getSignMessage()

    fun createDelegate(device: LedgerDevice): LedgerDeviceMapperDelegate {
        return when (device.deviceType) {
            LedgerDeviceType.STAX -> LedgerStaxMapperDelegate(resourceManager, device)
            LedgerDeviceType.FLEX -> LedgerFlexMapperDelegate(resourceManager, device)
            LedgerDeviceType.NANO_X -> LedgerNanoXUIMapperDelegate(resourceManager, device)
            LedgerDeviceType.NANO_S_PLUS -> LedgerNanoSPlusMapperDelegate(resourceManager, device)
        }
    }
}

interface LedgerDeviceMapperDelegate {

    fun getName(): String

    fun getApproveImage(): LedgerMessageCommand.Graphics

    fun getErrorImage(): LedgerMessageCommand.Graphics

    fun getSignImage(): LedgerMessageCommand.Graphics

    fun getReviewAddressMessage(): String

    fun getSignMessage(): String
}
