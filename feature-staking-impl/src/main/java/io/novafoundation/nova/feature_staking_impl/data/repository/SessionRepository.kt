package io.novafoundation.nova.feature_staking_impl.data.repository

import io.novafoundation.nova.common.utils.session
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindCurrentIndex
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.queryNonNull
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import java.math.BigInteger

interface SessionRepository {

    suspend fun currentSessionIndex(chainId: ChainId): BigInteger
}

class RealSessionRepository(
    private val remoteStorage: StorageDataSource
) : SessionRepository {

    override suspend fun currentSessionIndex(chainId: ChainId) = remoteStorage.queryNonNull(
        // Current session index
        keyBuilder = { it.metadata.session().storage("CurrentIndex").storageKey() },
        binding = ::bindCurrentIndex,
        chainId = chainId
    )
}
