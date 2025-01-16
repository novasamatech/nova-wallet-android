package io.novafoundation.nova.feature_staking_impl.data.mythos.repository

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_staking_impl.data.repository.consensus.AuraSession
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface MythosSessionRepository {

    suspend fun sessionLength(chain: Chain): BlockNumber

    fun currentSlotFlow(chainId: ChainId): Flow<BlockNumber>
}

@FeatureScope
class RealMythosSessionRepository @Inject constructor(
    private val auraSession: AuraSession,
): MythosSessionRepository {

    override suspend fun sessionLength(chain: Chain): BlockNumber {
        return chain.additional?.sessionLength?.toBigInteger()
            ?: auraSession.sessionLength(chain.id)
    }

    override fun currentSlotFlow(chainId: ChainId): Flow<BlockNumber> {
        return auraSession.currentSlotFlow(chainId)
    }
}
