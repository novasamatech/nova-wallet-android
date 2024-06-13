package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.finish

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.createName.CreateWalletNameViewModel
import io.novafoundation.nova.feature_ledger_impl.domain.account.connect.generic.finish.FinishImportGenericLedgerInteractor
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.common.payload.toDomain
import kotlinx.coroutines.launch

class FinishImportGenericLedgerViewModel(
    private val router: LedgerRouter,
    private val resourceManager: ResourceManager,
    private val payload: FinishImportGenericLedgerPayload,
    private val accountInteractor: AccountInteractor,
    private val interactor: FinishImportGenericLedgerInteractor
) : CreateWalletNameViewModel(router, resourceManager) {

    override fun proceed(name: String) {
        launch {
            interactor.createWallet(name, payload.account.toDomain())
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
}
