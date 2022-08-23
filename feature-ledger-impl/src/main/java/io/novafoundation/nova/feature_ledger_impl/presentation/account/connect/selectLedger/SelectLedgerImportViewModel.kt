package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.selectLedger

import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerViewModel

class SelectLedgerImportViewModel(
    substrateApplication: SubstrateLedgerApplication,
    discoveryService: LedgerDeviceDiscoveryService,
    permissionsAsker: PermissionsAsker.Presentation,
    bluetoothManager: BluetoothManager,
    router: LedgerRouter,
) : SelectLedgerViewModel(
    substrateApplication = substrateApplication,
    discoveryService = discoveryService,
    permissionsAsker = permissionsAsker,
    bluetoothManager = bluetoothManager,
    router = router
) {

    override fun connectedToDevice(device: LedgerDevice, account: LedgerSubstrateAccount) {
        showMessage("Connected to device ${device.name} with account ${account.address}")
    }
}
