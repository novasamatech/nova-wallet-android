package io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.legacy.selectLedger

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.location.LocationManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.domain.migration.LedgerMigrationUseCase
import io.novafoundation.nova.feature_ledger_impl.domain.migration.determineAppForLegacyAccount
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.legacy.selectAddress.AddLedgerChainAccountSelectAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.mappers.LedgerDeviceFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerViewModel

class AddChainAccountSelectLedgerViewModel(
    private val migrationUseCase: LedgerMigrationUseCase,
    private val router: LedgerRouter,
    private val payload: AddChainAccountSelectLedgerPayload,
    private val messageCommandFormatter: MessageCommandFormatter,
    discoveryService: LedgerDeviceDiscoveryService,
    permissionsAsker: PermissionsAsker.Presentation,
    bluetoothManager: BluetoothManager,
    locationManager: LocationManager,
    resourceManager: ResourceManager,
    messageFormatter: LedgerMessageFormatter,
    ledgerDeviceFormatter: LedgerDeviceFormatter
) : SelectLedgerViewModel(
    discoveryService = discoveryService,
    permissionsAsker = permissionsAsker,
    bluetoothManager = bluetoothManager,
    locationManager = locationManager,
    router = router,
    resourceManager = resourceManager,
    messageFormatter = messageFormatter,
    ledgerDeviceFormatter = ledgerDeviceFormatter,
    messageCommandFormatter = messageCommandFormatter,
    payload = payload
) {

    private val addAccountPayload = payload.addAccountPayload

    override suspend fun verifyConnection(device: LedgerDevice) {
        ledgerMessageCommands.value = messageCommandFormatter.hideCommand().event()

        val app = migrationUseCase.determineAppForLegacyAccount(addAccountPayload.chainId)

        // ensure that address loads successfully
        app.getSubstrateAccount(device, addAccountPayload.chainId, accountIndex = 0, confirmAddress = false)

        val payload = AddLedgerChainAccountSelectAddressPayload(addAccountPayload.chainId, addAccountPayload.metaId, device.id)
        router.openAddChainAccountSelectAddress(payload)
    }
}
