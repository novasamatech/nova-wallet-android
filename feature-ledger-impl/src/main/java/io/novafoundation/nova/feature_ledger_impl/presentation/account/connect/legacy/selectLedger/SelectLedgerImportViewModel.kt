package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectLedger

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.location.LocationManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectLedgerAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

class SelectLedgerImportViewModel(
    private val substrateApplication: SubstrateLedgerApplication,
    private val selectLedgerPayload: SelectLedgerPayload,
    private val router: LedgerRouter,
    discoveryService: LedgerDeviceDiscoveryService,
    permissionsAsker: PermissionsAsker.Presentation,
    bluetoothManager: BluetoothManager,
    locationManager: LocationManager,
    resourceManager: ResourceManager,
    chainRegistry: ChainRegistry,
) : SelectLedgerViewModel(
    discoveryService = discoveryService,
    permissionsAsker = permissionsAsker,
    bluetoothManager = bluetoothManager,
    locationManager = locationManager,
    router = router,
    resourceManager = resourceManager,
    chainRegistry = chainRegistry,
    payload = selectLedgerPayload
) {

    override suspend fun verifyConnection(device: LedgerDevice) {
        ledgerMessageCommands.value = LedgerMessageCommand.Hide.event()

        // ensure that address loads successfully
        substrateApplication.getAccount(device, selectLedgerPayload.chainId, accountIndex = 0, confirmAddress = false)

        val selectAddressPayload = SelectLedgerAddressPayload(device.id, selectLedgerPayload.chainId)
        router.openSelectImportAddress(selectAddressPayload)
    }
}
