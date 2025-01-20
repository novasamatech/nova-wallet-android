package io.novafoundation.nova.feature_staking_impl.data.repository

import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.currentIndex
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.session
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.validators
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.SessionValidators
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.api.observeNonNull
import io.novafoundation.nova.runtime.storage.source.query.api.queryNonNull
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

interface SessionRepository {

    fun observeCurrentSessionIndex(chainId: ChainId): Flow<BigInteger>

    fun sessionValidatorsFlow(chainId: ChainId): Flow<SessionValidators>

    suspend fun getSessionValidators(chainId: ChainId): SessionValidators
}

class RealSessionRepository(
    private val localStorage: StorageDataSource,
) : SessionRepository {

    override fun observeCurrentSessionIndex(chainId: ChainId) = localStorage.subscribe(chainId) {
        metadata.session.currentIndex.observeNonNull()
    }

    override fun sessionValidatorsFlow(chainId: ChainId): Flow<SessionValidators> {
        return localStorage.subscribe(chainId) {
            metadata.session.validators.observeNonNull()
        }
    }

    override suspend fun getSessionValidators(chainId: ChainId): SessionValidators {
        return localStorage.query(chainId) {
            metadata.session.validators.queryNonNull()
        }
    }
}
