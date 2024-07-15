package io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.dao.ExternalBalanceDao
import io.novafoundation.nova.core_db.dao.updateExternalBalance
import io.novafoundation.nova.core_db.model.ExternalBalanceLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_staking_api.data.network.blockhain.updaters.PooledBalanceUpdaterFactory
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.bondedAccountOf
import io.novafoundation.nova.feature_staking_api.domain.model.activeBalance
import io.novafoundation.nova.feature_staking_api.domain.nominationPool.model.PoolId
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.ledger
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.staking
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.bondedPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.delegatedStakingOrNull
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.delegators
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.nominationPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.poolMembers
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.subPoolsStorage
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.BondedPool
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolPoints
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.UnbondingPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.totalUnbondingFor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.PoolBalanceConvertable
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.amountOf
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.observeNonNull
import io.novafoundation.nova.runtime.storage.source.query.metadata
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class RealPooledBalanceUpdaterFactory(
    private val remoteStorageSource: StorageDataSource,
    private val poolAccountDerivation: PoolAccountDerivation,
    private val externalBalanceDao: ExternalBalanceDao,
    private val scope: AccountUpdateScope,
) : PooledBalanceUpdaterFactory {

    override fun create(chain: Chain): Updater<MetaAccount> {
        return PooledBalanceUpdater(
            scope = scope,
            chain = chain,
            remoteStorageSource = remoteStorageSource,
            poolAccountDerivation = poolAccountDerivation,
            externalBalanceDao = externalBalanceDao
        )
    }
}

class PooledBalanceUpdater(
    override val scope: AccountUpdateScope,
    private val chain: Chain,
    private val remoteStorageSource: StorageDataSource,
    private val poolAccountDerivation: PoolAccountDerivation,
    private val externalBalanceDao: ExternalBalanceDao,
) : Updater<MetaAccount> {

    val chainAsset = chain.utilityAsset

    override val requiredModules: List<String> = emptyList()

    override suspend fun listenForUpdates(
        storageSubscriptionBuilder: SharedRequestsBuilder,
        scopeValue: MetaAccount,
    ): Flow<Updater.SideEffect> {
        return if (chainAsset.enabled && StakingType.NOMINATION_POOLS in chainAsset.staking) {
            sync(storageSubscriptionBuilder, metaAccount = scopeValue)
        } else {
            emptyFlow()
        }
    }

    private suspend fun sync(storageSubscriptionBuilder: SharedRequestsBuilder, metaAccount: MetaAccount): Flow<Updater.SideEffect> {
        val accountId = metaAccount.accountIdIn(chain) ?: return emptyFlow()

        return remoteStorageSource.subscribe(chain.id, storageSubscriptionBuilder) {
            val poolMemberFlow = metadata.nominationPools.poolMembers
                .observe(accountId)

            val inAccountPoolStakeFlow = observeInAccountPoolStake(accountId)

            val poolWithBalance = poolMemberFlow
                .map { it?.poolId }
                .distinctUntilChanged()
                .flatMapLatest(::subscribeToPoolWithBalance)

            combine(poolMemberFlow, poolWithBalance, inAccountPoolStakeFlow) { poolMember, totalPoolBalances, inAccountPoolStake ->
                insertExternalBalance(poolMember, totalPoolBalances, metaAccount, inAccountPoolStake)
            }
        }.noSideAffects()
    }

    private suspend fun subscribeToPoolWithBalance(poolId: PoolId?): Flow<TotalPoolBalances?> {
        if (poolId == null) return flowOf(null)

        val bondedPoolAccountId = poolAccountDerivation.bondedAccountOf(poolId, chain.id)

        return remoteStorageSource.subscribeBatched(chain.id) {
            val bondedPoolFlow = metadata.nominationPools.bondedPools.observeNonNull(poolId.value)
            val unbondingPoolsFlow = metadata.nominationPools.subPoolsStorage.observe(poolId.value)

            val bondedPoolStakeFlow = metadata.staking.ledger
                .observeNonNull(bondedPoolAccountId)
                .map { it.activeBalance() }

            combine(bondedPoolFlow, unbondingPoolsFlow, bondedPoolStakeFlow, ::TotalPoolBalances)
        }
    }

    context(StorageQueryContext)
    @Suppress("IfThenToElvis")
    private fun observeInAccountPoolStake(accountId: AccountId): Flow<Balance> {
        val delegatedStakingPallet = metadata.delegatedStakingOrNull

        return if (delegatedStakingPallet != null) {
            delegatedStakingPallet.delegators.observe(accountId).map {
                it?.amount.orZero()
            }
        } else {
            flowOf(Balance.ZERO)
        }
    }

    private suspend fun insertExternalBalance(
        poolMember: PoolMember?,
        totalPoolBalances: TotalPoolBalances?,
        metaAccount: MetaAccount,
        inAccountPoolStake: Balance,
    ) {
        val totalStake = if (poolMember != null && totalPoolBalances != null) {
            // Only use stake part that is not in account as external balance entry
            totalPoolBalances.totalStakeOf(poolMember) - inAccountPoolStake
        } else {
            Balance.ZERO
        }

        val externalBalance = ExternalBalanceLocal(
            metaId = metaAccount.id,
            chainId = chain.id,
            assetId = chainAsset.id,
            type = ExternalBalanceLocal.Type.NOMINATION_POOL,
            subtype = ExternalBalanceLocal.EMPTY_SUBTYPE,
            amount = totalStake
        )

        externalBalanceDao.updateExternalBalance(externalBalance)
    }

    private class TotalPoolBalances(
        pool: BondedPool,
        val unbondingPools: UnbondingPools?,
        override val poolBalance: Balance,
    ) : PoolBalanceConvertable {

        override val poolPoints: PoolPoints = pool.points
    }

    private fun TotalPoolBalances.totalStakeOf(poolMember: PoolMember): Balance {
        val activeStake = amountOf(poolMember.points)
        val unbonding = unbondingPools.totalUnbondingFor(poolMember)

        return activeStake + unbonding
    }
}
