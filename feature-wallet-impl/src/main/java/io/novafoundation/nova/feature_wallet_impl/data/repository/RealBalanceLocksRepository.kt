package io.novafoundation.nova.feature_wallet_impl.data.repository

import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.LockDao
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLock
import io.novafoundation.nova.feature_wallet_api.domain.model.mapBalanceLockFromLocal
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RealBalanceLocksRepository(
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val lockDao: LockDao
) : BalanceLocksRepository {

    override suspend fun observeBalanceLocks(chain: Chain, chainAsset: Chain.Asset): Flow<List<BalanceLock>> {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        return lockDao.observeBalanceLocks(metaAccount.id, chain.id, chainAsset.id)
            .mapList { lock -> mapBalanceLockFromLocal(chainAsset, lock) }
    }

    override suspend fun getBiggestLock(chain: Chain, chainAsset: Chain.Asset): BalanceLock? {
        val metaAccount = accountRepository.getSelectedMetaAccount()

        return lockDao.getBiggestBalanceLock(metaAccount.id, chain.id, chainAsset.id)?.let {
            mapBalanceLockFromLocal(chainAsset, it)
        }
    }
    override suspend fun observeBalanceLock(chainAsset: Chain.Asset, lockId: String): Flow<BalanceLock?> {
        val metaAccount = accountRepository.getSelectedMetaAccount()

        return lockDao.observeBalanceLock(metaAccount.id, chainAsset.chainId, chainAsset.id, lockId).map { lockLocal ->
            lockLocal?.let { mapBalanceLockFromLocal(chainAsset, it) }
        }
    }

    override fun observeLocksForMetaAccount(metaAccount: MetaAccount): Flow<List<BalanceLock>> {
        return combine(lockDao.observeLocksForMetaAccount(metaAccount.id), chainRegistry.chainsById) { locks, chains ->
            locks.map {
                val asset = chains.getValue(it.chainId)
                    .assetsById.getValue(it.assetId)
                mapBalanceLockFromLocal(asset, it)
            }
        }
    }
}
