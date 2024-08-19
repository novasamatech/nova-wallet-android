package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.selectAddress

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.LedgerAccountWithBalance
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.SelectAddressLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectAddressLedgerViewModel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectLedgerAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.common.payload.toGenericParcel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.preview.PreviewImportGenericLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate.newApp.GenericSubstrateLedgerApplication
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.launch

class SelectAddressImportGenericLedgerViewModel(
    private val substrateApplication: GenericSubstrateLedgerApplication,
    private val router: LedgerRouter,
    private val payload: SelectLedgerAddressPayload,
    interactor: SelectAddressLedgerInteractor,
    addressIconGenerator: AddressIconGenerator,
    resourceManager: ResourceManager,
    chainRegistry: ChainRegistry,
    messageFormatter: LedgerMessageFormatter
) : SelectAddressLedgerViewModel(
    router = router,
    interactor = interactor,
    addressIconGenerator = addressIconGenerator,
    resourceManager = resourceManager,
    payload = payload,
    chainRegistry = chainRegistry,
    messageFormatter = messageFormatter
) {

    override val needToVerifyAccount = false

    override fun onAccountVerified(account: LedgerAccountWithBalance) {
        launch {
            val payload = PreviewImportGenericLedgerPayload(
                accountIndex = account.index,
                account = account.account.toGenericParcel(),
                deviceId = payload.deviceId
            )

            router.openPreviewLedgerAccountsGeneric(payload)
        }
    }
}
