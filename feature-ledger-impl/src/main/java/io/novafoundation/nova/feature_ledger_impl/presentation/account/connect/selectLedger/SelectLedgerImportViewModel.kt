package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.selectLedger

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

class SelectLedgerImportViewModel(
    private val substrateApplication: SubstrateLedgerApplication,
    private val selectLedgerPayload: SelectLedgerPayload,
    discoveryService: LedgerDeviceDiscoveryService,
    permissionsAsker: PermissionsAsker.Presentation,
    bluetoothManager: BluetoothManager,
    router: LedgerRouter,
    resourceManager: ResourceManager,
    chainRegistry: ChainRegistry,
) : SelectLedgerViewModel(
    substrateApplication = substrateApplication,
    discoveryService = discoveryService,
    permissionsAsker = permissionsAsker,
    bluetoothManager = bluetoothManager,
    router = router,
    resourceManager = resourceManager,
    chainRegistry = chainRegistry,
    payload = selectLedgerPayload
) {

    override suspend fun verifyConnection(device: LedgerDevice) {
        val account = substrateApplication.getAccount(device, selectLedgerPayload.chainId, accountIndex = 0)

        showMessage("Connected to device ${device.name} with account ${account.address}")
    }
}
