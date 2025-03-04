package io.novafoundation.nova.feature_staking_impl.data.mythos.repository

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.candidates
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.collatorStaking
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.MythCandidateInfos
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Inject
import javax.inject.Named

interface MythosCandidatesRepository {

    suspend fun getCandidateInfos(chainId: ChainId): MythCandidateInfos
}

@FeatureScope
class RealMythosCandidatesRepository @Inject constructor(
    @Named(REMOTE_STORAGE_SOURCE)
    private val remoteStorageSource: StorageDataSource
) : MythosCandidatesRepository {

    override suspend fun getCandidateInfos(chainId: ChainId): MythCandidateInfos {
        return remoteStorageSource.query(chainId) {
            metadata.collatorStaking.candidates.entries()
        }
    }
}
