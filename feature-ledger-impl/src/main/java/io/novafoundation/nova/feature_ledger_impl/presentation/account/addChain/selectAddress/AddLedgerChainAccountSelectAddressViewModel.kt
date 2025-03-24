package io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectAddress

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.model.LedgerVariant
import io.novafoundation.nova.feature_ledger_impl.domain.account.addChain.AddLedgerChainAccountInteractor
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.LedgerAccountWithBalance
import io.novafoundation.nova.feature_ledger_impl.domain.account.common.selectAddress.SelectAddressLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectAddressLedgerViewModel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectLedgerAddressPayload
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
    messageCommandFormatter: MessageCommandFormatter
) : SelectAddressLedgerViewModel(
    router = router,
    interactor = selectAddressLedgerInteractor,
    addressIconGenerator = addressIconGenerator,
    resourceManager = resourceManager,
    payload = selectLedgerAddressPayload,
    chainRegistry = chainRegistry,
    messageCommandFormatter = messageCommandFormatter
) {

    override val ledgerVariant: LedgerVariant = LedgerVariant.LEGACY

    override fun onAccountVerified(account: LedgerAccountWithBalance) {
        launch {
            val result = withContext(Dispatchers.Default) {
                addChainAccountInteractor.addChainAccount(payload.metaId, payload.chainId, account.account)
            }

            result.onSuccess {
                router.openMain()
            }.onFailure(::showError)
        }
    }
}
