package io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.legacy.selectAddress

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.format.AddressScheme
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.model.LedgerVariant
import io.novafoundation.nova.feature_account_api.presenatation.addressActions.AddressActionsMixin
import io.novafoundation.nova.feature_ledger_impl.domain.account.addChain.legacy.AddLedgerChainAccountInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.LedgerAccount
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.SelectAddressLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectAddressLedgerViewModel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectLedgerAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.model.AddressVerificationMode
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddLedgerChainAccountSelectAddressViewModel(
    private val router: LedgerRouter,
    private val payload: AddLedgerChainAccountSelectAddressPayload,
    private val addChainAccountInteractor: AddLedgerChainAccountInteractor,
    selectAddressLedgerInteractor: SelectAddressLedgerInteractor,
    addressIconGenerator: AddressIconGenerator,
    resourceManager: ResourceManager,
    chainRegistry: ChainRegistry,
    selectLedgerAddressPayload: SelectLedgerAddressPayload,
    messageCommandFormatter: MessageCommandFormatter,
    addressActionsMixinFactory: AddressActionsMixin.Factory
) : SelectAddressLedgerViewModel(
    router = router,
    interactor = selectAddressLedgerInteractor,
    addressIconGenerator = addressIconGenerator,
    resourceManager = resourceManager,
    payload = selectLedgerAddressPayload,
    chainRegistry = chainRegistry,
    messageCommandFormatter = messageCommandFormatter,
    addressActionsMixinFactory = addressActionsMixinFactory
) {

    override val ledgerVariant: LedgerVariant = LedgerVariant.LEGACY

    override val addressVerificationMode = AddressVerificationMode.Enabled(addressSchemesToVerify = listOf(AddressScheme.SUBSTRATE))

    override fun onAccountVerified(account: LedgerAccount) {
        launch {
            val result = withContext(Dispatchers.Default) {
                addChainAccountInteractor.addChainAccount(payload.metaId, payload.chainId, account.substrate)
            }

            result.onSuccess {
                router.openMain()
            }.onFailure(::showError)
        }
    }
}
