package io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectLedger

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectAddress.AddLedgerChainAccountSelectAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

class AddChainAccountSelectLedgerViewModel(
    private val substrateApplication: SubstrateLedgerApplication,
    private val router: LedgerRouter,
    private val addAccountPayload: AddAccountPayload.ChainAccount,
    discoveryService: LedgerDeviceDiscoveryService,
    permissionsAsker: PermissionsAsker.Presentation,
    bluetoothManager: BluetoothManager,
    resourceManager: ResourceManager,
    chainRegistry: ChainRegistry,
    selectLedgerPayload: SelectLedgerPayload,
) : SelectLedgerViewModel(
    discoveryService = discoveryService,
    permissionsAsker = permissionsAsker,
    bluetoothManager = bluetoothManager,
    router = router,
    resourceManager = resourceManager,
    chainRegistry = chainRegistry,
    payload = selectLedgerPayload
) {

    override suspend fun verifyConnection(device: LedgerDevice) {
        ledgerMessageCommands.value = LedgerMessageCommand.Hide.event()

        // ensure that address loads successfully
        substrateApplication.getAccount(device, addAccountPayload.chainId, accountIndex = 0, confirmAddress = false)

        val payload = AddLedgerChainAccountSelectAddressPayload(addAccountPayload.chainId, addAccountPayload.metaId, device.id)
        router.openAddChainAccountSelectAddress(payload)
    }
}
