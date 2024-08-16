package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectAddress

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.LedgerAccountWithBalance
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.SelectAddressLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectAddressLedgerViewModel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectLedgerAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.LedgerChainAccount
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.SelectLedgerAddressInterScreenResponder
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

class SelectAddressImportLedgerLegacyViewModel(
    private val router: LedgerRouter,
    private val payload: SelectLedgerAddressPayload,
    private val responder: SelectLedgerAddressInterScreenResponder,
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

    override fun onAccountVerified(account: LedgerAccountWithBalance) {
        responder.respond(screenResponseFrom(account))
        router.returnToImportFillWallet()
    }

    private fun screenResponseFrom(account: LedgerAccountWithBalance): LedgerChainAccount {
        return LedgerChainAccount(
            publicKey = account.account.publicKey,
            address = account.account.address,
            chainId = payload.chainId,
            encryptionType = account.account.encryptionType,
            derivationPath = account.account.derivationPath
        )
    }
}
