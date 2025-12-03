package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.finish

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.createName.CreateWalletNameViewModel
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.finish.FinishImportParitySignerInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.ParitySignerAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.toDomain

class FinishImportParitySignerViewModel(
    private val router: AccountRouter,
    private val resourceManager: ResourceManager,
    private val payload: ParitySignerAccountPayload,
    private val accountInteractor: AccountInteractor,
    private val interactor: FinishImportParitySignerInteractor
) : CreateWalletNameViewModel(router, resourceManager) {

    override fun proceed(name: String) = launchUnit {
        val result = when (payload) {
            is ParitySignerAccountPayload.AsPublic -> interactor.createPolkadotVaultWallet(
                name,
                payload.accountId,
                payload.variant
            )

            is ParitySignerAccountPayload.AsSecret -> interactor.createSecretWallet(
                name,
                secret = payload.secret.toDomain(),
                payload.variant
            )
        }

        result.onSuccess { continueBasedOnCodeStatus() }
            .onFailure(::showError)
    }

    private suspend fun continueBasedOnCodeStatus() {
        if (accountInteractor.isCodeSet()) {
            router.openMain()
        } else {
            router.openCreatePincode()
        }
    }
}
