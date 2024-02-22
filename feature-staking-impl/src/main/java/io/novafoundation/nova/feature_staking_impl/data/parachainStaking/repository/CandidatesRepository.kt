package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository

import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CandidateMetadata
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.bindCandidateMetadata
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.wrapSingleArgumentKeys
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.storage

interface CandidatesRepository {

    suspend fun getCandidateMetadata(chainId: ChainId, collatorId: AccountId): CandidateMetadata

    suspend fun getCandidatesMetadata(chainId: ChainId, collatorIds: Collection<AccountId>): AccountIdMap<CandidateMetadata>
}

class RealCandidatesRepository(
    private val storageDataSource: StorageDataSource
) : CandidatesRepository {

    override suspend fun getCandidateMetadata(chainId: ChainId, collatorId: AccountId): CandidateMetadata {
        return storageDataSource.query(chainId) {
            runtime.metadata.parachainStaking().storage("CandidateInfo").query(collatorId, binding = ::bindCandidateMetadata)
        }
    }

    override suspend fun getCandidatesMetadata(chainId: ChainId, collatorIds: Collection<AccountId>): AccountIdMap<CandidateMetadata> {
        return storageDataSource.query(chainId) {
            runtime.metadata.parachainStaking().storage("CandidateInfo").entries(
                keysArguments = collatorIds.wrapSingleArgumentKeys(),
                keyExtractor = { (accountId: AccountId) -> accountId.toHexString() },
                binding = { instance, _ -> bindCandidateMetadata(instance) }
            )
        }
    }
}
