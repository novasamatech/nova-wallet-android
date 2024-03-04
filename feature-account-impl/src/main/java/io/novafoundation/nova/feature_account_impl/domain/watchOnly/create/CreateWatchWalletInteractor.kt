package io.novafoundation.nova.feature_account_impl.domain.watchOnly.create

import io.novafoundation.nova.common.utils.ethereumAddressToAccountId
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.WatchOnlyRepository
import io.novafoundation.nova.feature_account_impl.data.repository.WatchWalletSuggestion
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.watchOnly.WatchOnlyAddAccountRepository
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId

interface CreateWatchWalletInteractor {

    suspend fun createWallet(
        name: String,
        substrateAddress: String,
        evmAddress: String
    ): Result<*>

    suspend fun suggestions(): List<WatchWalletSuggestion>
}

class RealCreateWatchWalletInteractor(
    private val repository: WatchOnlyRepository,
    private val watchOnlyAddAccountRepository: WatchOnlyAddAccountRepository,
    private val accountRepository: AccountRepository,
) : CreateWatchWalletInteractor {

    override suspend fun createWallet(name: String, substrateAddress: String, evmAddress: String) = runCatching {
        val substrateAccountId = substrateAddress.toAccountId()
        val evmAccountId = evmAddress.takeIf { it.isNotEmpty() }?.ethereumAddressToAccountId()

        val metaId = watchOnlyAddAccountRepository.addAccount(
            WatchOnlyAddAccountRepository.Payload.MetaAccount(
                name,
                substrateAccountId,
                evmAccountId
            )
        )

        accountRepository.selectMetaAccount(metaId)
    }

    override suspend fun suggestions(): List<WatchWalletSuggestion> {
        return repository.watchWalletSuggestions()
    }
}
