package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectAddress

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.format.AddressScheme
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.model.LedgerVariant
import io.novafoundation.nova.feature_account_api.presenatation.addressActions.AddressActionsMixin
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.LedgerAccount
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.SelectAddressLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectAddressLedgerViewModel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectLedgerAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.model.AddressVerificationMode
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
    messageCommandFormatter: MessageCommandFormatter,
    addressActionsMixinFactory: AddressActionsMixin.Factory
) : SelectAddressLedgerViewModel(
    router = router,
    interactor = interactor,
    addressIconGenerator = addressIconGenerator,
    resourceManager = resourceManager,
    payload = payload,
    chainRegistry = chainRegistry,
    messageCommandFormatter = messageCommandFormatter,
    addressActionsMixinFactory = addressActionsMixinFactory
) {

    override val ledgerVariant: LedgerVariant = LedgerVariant.LEGACY

    override val addressVerificationMode = AddressVerificationMode.Enabled(addressSchemesToVerify = listOf(AddressScheme.SUBSTRATE))

    override fun onAccountVerified(account: LedgerAccount) {
        responder.respond(screenResponseFrom(account))
        router.returnToImportFillWallet()
    }

    private fun screenResponseFrom(account: LedgerAccount): LedgerChainAccount {
        return LedgerChainAccount(
            publicKey = account.substrate.publicKey,
            address = account.substrate.address,
            chainId = payload.substrateChainId,
            encryptionType = account.substrate.encryptionType,
            derivationPath = account.substrate.derivationPath
        )
    }
}
