package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectLedger

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.location.LocationManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.domain.migration.LedgerMigrationUseCase
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectLedgerAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerViewModel

class SelectLedgerImportViewModel(
    private val migrationUseCase: LedgerMigrationUseCase,
    private val selectLedgerPayload: SelectLedgerPayload,
    private val router: LedgerRouter,
    discoveryService: LedgerDeviceDiscoveryService,
    permissionsAsker: PermissionsAsker.Presentation,
    bluetoothManager: BluetoothManager,
    locationManager: LocationManager,
    resourceManager: ResourceManager,
    messageFormatter: LedgerMessageFormatter
) : SelectLedgerViewModel(
    discoveryService = discoveryService,
    permissionsAsker = permissionsAsker,
    bluetoothManager = bluetoothManager,
    locationManager = locationManager,
    router = router,
    resourceManager = resourceManager,
    messageFormatter = messageFormatter
) {

    override suspend fun verifyConnection(device: LedgerDevice) {
        ledgerMessageCommands.value = LedgerMessageCommand.Hide.event()

        val app = migrationUseCase.determineAppForLegacyAccount(selectLedgerPayload.chainId)

        // ensure that address loads successfully
        app.getAccount(device, selectLedgerPayload.chainId, accountIndex = 0, confirmAddress = false)

        val selectAddressPayload = SelectLedgerAddressPayload(device.id, selectLedgerPayload.chainId)
        router.openSelectImportAddress(selectAddressPayload)
    }
}
