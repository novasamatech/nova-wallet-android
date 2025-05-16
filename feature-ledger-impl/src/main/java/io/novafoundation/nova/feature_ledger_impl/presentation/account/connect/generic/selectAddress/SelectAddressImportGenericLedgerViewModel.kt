package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.selectAddress

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.model.LedgerVariant
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.LedgerAccount
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.SelectAddressLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectAddressLedgerViewModel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectLedgerAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.common.payload.toGenericParcel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.common.payload.toParcel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.preview.PreviewImportGenericLedgerPayload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.launch

class SelectAddressImportGenericLedgerViewModel(
    private val router: LedgerRouter,
    private val payload: SelectLedgerAddressPayload,
    interactor: SelectAddressLedgerInteractor,
    addressIconGenerator: AddressIconGenerator,
    resourceManager: ResourceManager,
    chainRegistry: ChainRegistry,
    messageCommandFormatter: MessageCommandFormatter,
) : SelectAddressLedgerViewModel(
    router = router,
    interactor = interactor,
    addressIconGenerator = addressIconGenerator,
    resourceManager = resourceManager,
    payload = payload,
    chainRegistry = chainRegistry,
    messageCommandFormatter = messageCommandFormatter
) {

    override val ledgerVariant: LedgerVariant = LedgerVariant.GENERIC

    override val needToVerifyAccount = false

    override fun onAccountVerified(account: LedgerAccount) {
        launch {
            val payload = PreviewImportGenericLedgerPayload(
                accountIndex = account.index,
                substrateAccount = account.substrate.toGenericParcel(),
                evmAccount = account.evm?.toParcel(),
                deviceId = payload.deviceId
            )

            router.openPreviewLedgerAccountsGeneric(payload)
        }
    }
}
