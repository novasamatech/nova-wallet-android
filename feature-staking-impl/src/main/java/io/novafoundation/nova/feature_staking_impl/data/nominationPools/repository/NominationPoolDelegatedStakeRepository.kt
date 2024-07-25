package io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository

import io.novafoundation.nova.common.utils.delegatedStakingOrNull
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.delegatedStaking
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.delegators
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.DelegatedStakeMigrationState
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.metadata
import io.novasama.substrate_sdk_android.runtime.AccountId

interface NominationPoolDelegatedStakeRepository {

    suspend fun hasMigratedToDelegatedStake(chainId: ChainId): Boolean

    suspend fun poolMemberMigrationState(chainId: ChainId, accountId: AccountId): DelegatedStakeMigrationState
}

suspend fun NominationPoolDelegatedStakeRepository.shouldMigrateToDelegatedStake(chainId: ChainId, accountId: AccountId): Boolean {
    return poolMemberMigrationState(chainId, accountId) == DelegatedStakeMigrationState.NEEDS_MIGRATION
}

class RealNominationPoolDelegatedStakeRepository(
    private val localStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
) : NominationPoolDelegatedStakeRepository {

    override suspend fun hasMigratedToDelegatedStake(chainId: ChainId): Boolean {
        return chainRegistry.getRuntime(chainId).metadata.delegatedStakingOrNull() != null
    }

    override suspend fun poolMemberMigrationState(chainId: ChainId, accountId: AccountId): DelegatedStakeMigrationState {
        return when {
            !hasMigratedToDelegatedStake(chainId) -> DelegatedStakeMigrationState.NOT_SUPPORTED
            hasDelegatedStake(chainId, accountId) -> DelegatedStakeMigrationState.MIGRATED
            else -> DelegatedStakeMigrationState.NEEDS_MIGRATION
        }
    }

    private suspend fun hasDelegatedStake(chainId: ChainId, accountId: AccountId): Boolean {
        val delegatedStake = localStorageSource.query(chainId) {
            metadata.delegatedStaking.delegators.query(accountId)
        }

        return delegatedStake != null
    }
}
