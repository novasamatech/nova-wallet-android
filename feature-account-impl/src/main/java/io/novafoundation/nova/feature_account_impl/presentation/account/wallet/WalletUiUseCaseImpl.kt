package io.novafoundation.nova.feature_account_impl.presentation.account.wallet

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

class WalletUiUseCaseImpl(
    private val accountRepository: AccountRepository
) : WalletUiUseCase {

    override fun selectedWalletUiFlow(): Flow<WalletModel> {
        return accountRepository.selectedMetaAccountFlow().mapLatest {
            mapMetaAccountToWalletModel(it)
        }
    }

    private fun mapMetaAccountToWalletModel(
        metaAccount: MetaAccount
    ) = WalletModel(
        name = metaAccount.name
    )
}
