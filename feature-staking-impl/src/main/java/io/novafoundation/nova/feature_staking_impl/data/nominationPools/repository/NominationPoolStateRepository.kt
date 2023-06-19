package io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_staking_api.domain.model.Nominations
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.ledger
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.nominators
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.staking
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.metadata
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface NominationPoolStateRepository {

    context(StorageQueryContext)
    suspend fun observeBondedBalance(poolAccount: AccountId): Flow<Balance>

    context(StorageQueryContext)
    suspend fun observePoolNominations(poolAccount: AccountId): Flow<Nominations?>
}

class RealNominationPoolStateRepository() : NominationPoolStateRepository {

    context(StorageQueryContext)
    override suspend fun observeBondedBalance(poolAccount: AccountId): Flow<Balance> {
        return metadata.staking.ledger.observe(poolAccount).map { it?.active.orZero() }
    }

    context(StorageQueryContext)
    override suspend fun observePoolNominations(poolAccount: AccountId): Flow<Nominations?> {
        return metadata.staking.nominators.observe(poolAccount)
    }
}
