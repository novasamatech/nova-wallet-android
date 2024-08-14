package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.finish

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.createName.CreateWalletNameViewModel
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.feature_ledger_impl.domain.account.connect.legacy.finish.FinishImportLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.launch

class FinishImportLedgerViewModel(
    private val router: LedgerRouter,
    private val resourceManager: ResourceManager,
    private val payload: FinishImportLedgerPayload,
    private val accountInteractor: AccountInteractor,
    private val interactor: FinishImportLedgerInteractor
) : CreateWalletNameViewModel(router, resourceManager) {

    override fun proceed(name: String) {
        launch {
            interactor.createWallet(name, constructAccountsMap())
                .onSuccess { continueBasedOnCodeStatus() }
                .onFailure(::showError)
        }
    }

    private suspend fun continueBasedOnCodeStatus() {
        if (accountInteractor.isCodeSet()) {
            router.openMain()
        } else {
            router.openCreatePincode()
        }
    }

    private fun constructAccountsMap(): Map<ChainId, LedgerSubstrateAccount> = payload.ledgerChainAccounts.associateBy(
        keySelector = { it.chainId },
        valueTransform = { LedgerSubstrateAccount(it.index, it.address, it.publicKey, it.encryptionType, it.derivationPath) }
    )
}
