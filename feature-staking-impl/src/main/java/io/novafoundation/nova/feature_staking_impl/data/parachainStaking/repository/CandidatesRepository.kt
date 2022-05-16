package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository

import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CandidateMetadata
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.bindCandidateMetadata
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage

interface CandidatesRepository {

    suspend fun getCandidateMetadata(chainId: ChainId, collatorId: AccountId): CandidateMetadata
}

class RealCandidatesRepository(
    private val storageDataSource: StorageDataSource
) : CandidatesRepository {
    override suspend fun getCandidateMetadata(chainId: ChainId, collatorId: AccountId): CandidateMetadata {
        return storageDataSource.query(chainId) {
            runtime.metadata.parachainStaking().storage("CandidateInfo").query(collatorId, binding = ::bindCandidateMetadata)
        }
    }
}
