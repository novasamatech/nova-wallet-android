package io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.ledger
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.staking
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.PoolAccountDerivation.PoolAccountType
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.metadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface NominationPoolBalanceRepository {

    context(StorageQueryContext)
    suspend fun observeBondedBalance(poolId: PoolId): Flow<Balance>
}

class RealNominationPoolBalanceRepository(
    private val poolAccountDerivation: PoolAccountDerivation
): NominationPoolBalanceRepository {

    context(StorageQueryContext)
    override suspend fun observeBondedBalance(poolId: PoolId): Flow<Balance> {
        val poolAccount = poolAccountDerivation.derivePoolAccount(poolId, PoolAccountType.BONDED, chainId)

        return metadata.staking.ledger.observe(poolAccount).map { it?.active.orZero() }
    }
}
