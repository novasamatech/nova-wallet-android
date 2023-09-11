package io.novafoundation.nova.feature_assets.domain.assets

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.ExternalBalanceRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.ExternalBalance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

interface ExternalBalancesInteractor {

    fun observeExternalBalances(): Flow<List<ExternalBalance>>

    fun observeExternalBalances(assetId: FullChainAssetId): Flow<List<ExternalBalance>>
}

class RealExternalBalancesInteractor(
    private val accountRepository: AccountRepository,
    private val externalBalanceRepository: ExternalBalanceRepository,
) : ExternalBalancesInteractor {

    override fun observeExternalBalances(): Flow<List<ExternalBalance>> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
            externalBalanceRepository.observeAccountExternalBalances(metaAccount.id)
        }
    }

    override fun observeExternalBalances(assetId: FullChainAssetId): Flow<List<ExternalBalance>> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
            externalBalanceRepository.observeAccountChainExternalBalances(metaAccount.id, assetId)
        }
    }
}
