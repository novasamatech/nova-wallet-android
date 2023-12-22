package io.novafoundation.nova.feature_account_impl.domain.watchOnly.change

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.WatchOnlyRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.watchOnly.WatchOnlyAddAccountRepository
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface ChangeWatchAccountInteractor {

    suspend fun changeChainAccount(
        metaId: Long,
        chain: Chain,
        address: String
    ): Result<*>
}

class RealChangeWatchAccountInteractor(
    private val watchOnlyAddAccountRepository: WatchOnlyAddAccountRepository
) : ChangeWatchAccountInteractor {

    override suspend fun changeChainAccount(
        metaId: Long,
        chain: Chain,
        address: String
    ): Result<*> = runCatching {
        val accountId = chain.accountIdOf(address)

        watchOnlyAddAccountRepository.addAccount(
            WatchOnlyAddAccountRepository.Payload.ChainAccount(
                metaId = metaId,
                chainId = chain.id,
                accountId = accountId
            )
        )
    }
}
