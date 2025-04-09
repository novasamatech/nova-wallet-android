package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.selectLedger

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.location.LocationManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.mappers.LedgerDeviceFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectLedgerAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerViewModel
import io.novafoundation.nova.runtime.ext.ChainGeneses

class SelectLedgerGenericImportViewModel(
    private val router: LedgerRouter,
    private val messageCommandFormatter: MessageCommandFormatter,
    discoveryService: LedgerDeviceDiscoveryService,
    permissionsAsker: PermissionsAsker.Presentation,
    bluetoothManager: BluetoothManager,
    locationManager: LocationManager,
    resourceManager: ResourceManager,
    messageFormatter: LedgerMessageFormatter,
    payload: SelectLedgerPayload,
    deviceMapperFactory: LedgerDeviceFormatter,
) : SelectLedgerViewModel(
    discoveryService= discoveryService,
    permissionsAsker = permissionsAsker,
    bluetoothManager = bluetoothManager,
    locationManager = locationManager,
    router = router,
    resourceManager = resourceManager,
    messageFormatter = messageFormatter,
    ledgerDeviceFormatter = deviceMapperFactory,
    messageCommandFormatter = messageCommandFormatter,
    payload = payload
) {

    override suspend fun verifyConnection(device: LedgerDevice) {
        ledgerMessageCommands.value = messageCommandFormatter.hideCommand().event()

        val payload = SelectLedgerAddressPayload(
            deviceId = device.id,
            chainId = getPreviewBalanceChainId()
        )

        router.openSelectAddressGenericLedger(payload)
    }

    private fun getPreviewBalanceChainId() = ChainGeneses.POLKADOT
}
