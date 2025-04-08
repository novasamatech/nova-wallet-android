package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectLedger

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.location.LocationManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryServiceFactory
import io.novafoundation.nova.feature_ledger_impl.domain.migration.LedgerMigrationUseCase
import io.novafoundation.nova.feature_ledger_impl.domain.migration.determineAppForLegacyAccount
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.mappers.LedgerDeviceFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectLedgerAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerViewModel

class SelectLedgerLegacyImportViewModel(
    private val migrationUseCase: LedgerMigrationUseCase,
    private val selectLedgerPayload: SelectLedgerLegacyPayload,
    private val router: LedgerRouter,
    private val messageCommandFormatter: MessageCommandFormatter,
    discoveryServiceFactory: LedgerDeviceDiscoveryServiceFactory,
    permissionsAsker: PermissionsAsker.Presentation,
    bluetoothManager: BluetoothManager,
    locationManager: LocationManager,
    resourceManager: ResourceManager,
    messageFormatter: LedgerMessageFormatter,
    ledgerDeviceFormatter: LedgerDeviceFormatter
) : SelectLedgerViewModel(
    discoveryServiceFactory = discoveryServiceFactory,
    permissionsAsker = permissionsAsker,
    bluetoothManager = bluetoothManager,
    locationManager = locationManager,
    router = router,
    resourceManager = resourceManager,
    messageFormatter = messageFormatter,
    ledgerDeviceFormatter = ledgerDeviceFormatter,
    messageCommandFormatter = messageCommandFormatter,
    payload = selectLedgerPayload
) {

    override suspend fun verifyConnection(device: LedgerDevice) {
        ledgerMessageCommands.value = messageCommandFormatter.hideCommand().event()

        val app = migrationUseCase.determineAppForLegacyAccount(selectLedgerPayload.chainId)

        // ensure that address loads successfully
        app.getAccount(device, selectLedgerPayload.chainId, accountIndex = 0, confirmAddress = false)

        val selectAddressPayload = SelectLedgerAddressPayload(device.id, selectLedgerPayload.chainId)
        router.openSelectImportAddress(selectAddressPayload)
    }
}
