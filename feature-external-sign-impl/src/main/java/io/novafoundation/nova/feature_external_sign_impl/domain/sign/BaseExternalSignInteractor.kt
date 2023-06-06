package io.novafoundation.nova.feature_external_sign_impl.domain.sign

import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignWallet
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.Signer

abstract class BaseExternalSignInteractor(
    private val accountRepository: AccountRepository,
    private val wallet: ExternalSignWallet,
    private val signerProvider: SignerProvider,
) : ExternalSignInteractor {

    protected suspend fun resolveWalletSigner(): Signer {
        val metaAccount = resolveMetaAccount()

        return signerProvider.signerFor(metaAccount)
    }

    protected suspend fun resolveMetaAccount(): MetaAccount {
        return when (wallet) {
            ExternalSignWallet.Current -> accountRepository.getSelectedMetaAccount()
            is ExternalSignWallet.WithId -> accountRepository.getMetaAccount(wallet.metaId)
        }
    }
}
