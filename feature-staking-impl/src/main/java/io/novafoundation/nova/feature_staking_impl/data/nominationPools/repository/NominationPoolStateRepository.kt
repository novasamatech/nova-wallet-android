package io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository

import io.novafoundation.nova.common.utils.Perbill
import io.novafoundation.nova.feature_staking_api.domain.model.Nominations
import io.novafoundation.nova.feature_staking_api.domain.model.StakingLedger
import io.novafoundation.nova.feature_staking_api.domain.model.activeBalance
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.ledger
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.nominators
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.staking
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.bondedPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.nominationPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.BondedPool
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.WithRawValue
import io.novafoundation.nova.runtime.storage.source.query.api.observeNonNull
import io.novafoundation.nova.runtime.storage.source.query.metadata
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface NominationPoolStateRepository {

    context(StorageQueryContext)
    fun observeBondedPoolLedger(poolAccount: AccountId): Flow<WithRawValue<StakingLedger?>>

    context(StorageQueryContext)
    fun observePoolNominations(poolAccount: AccountId): Flow<WithRawValue<Nominations?>>

    fun observeParticipatingBondedBalance(poolAccount: AccountId, chainId: ChainId): Flow<Balance>

    fun observeParticipatingPoolNominations(poolAccount: AccountId, chainId: ChainId): Flow<Nominations?>

    fun observeParticipatingBondedPool(poolId: PoolId, chainId: ChainId): Flow<BondedPool>

    suspend fun getPoolCommissions(poolIds: Set<PoolId>, chainId: ChainId): Map<PoolId, Perbill?>
}

class RealNominationPoolStateRepository(
    private val localStorage: StorageDataSource,
    private val remoteStorage: StorageDataSource,
) : NominationPoolStateRepository {

    context(StorageQueryContext)
    override fun observeBondedPoolLedger(poolAccount: AccountId): Flow<WithRawValue<StakingLedger?>> {
        return metadata.staking.ledger.observeWithRaw(poolAccount)
    }

    context(StorageQueryContext)
    override fun observePoolNominations(poolAccount: AccountId): Flow<WithRawValue<Nominations?>> {
        return metadata.staking.nominators.observeWithRaw(poolAccount)
    }

    override fun observeParticipatingBondedBalance(poolAccount: AccountId, chainId: ChainId): Flow<Balance> {
        return localStorage.subscribe(chainId) {
            metadata.staking.ledger.observe(poolAccount).map { it.activeBalance() }
        }
    }

    override fun observeParticipatingPoolNominations(poolAccount: AccountId, chainId: ChainId): Flow<Nominations?> {
        return localStorage.subscribe(chainId) {
            metadata.staking.nominators.observe(poolAccount)
        }
    }

    override fun observeParticipatingBondedPool(poolId: PoolId, chainId: ChainId): Flow<BondedPool> {
        return localStorage.subscribe(chainId) {
            metadata.nominationPools.bondedPools.observeNonNull(poolId.value)
        }
    }

    override suspend fun getPoolCommissions(poolIds: Set<PoolId>, chainId: ChainId): Map<PoolId, Perbill?> {
        return remoteStorage.query(chainId) {
            metadata.nominationPools.bondedPools.multi(
                keys = poolIds.map { it.value },
                keyTransform = { PoolId(it) }
            )
                .mapValues { (_, bondedPool) -> bondedPool?.commission?.current?.perbill }
        }
    }
}
