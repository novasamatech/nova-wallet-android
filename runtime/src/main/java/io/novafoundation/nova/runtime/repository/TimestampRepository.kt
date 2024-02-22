package io.novafoundation.nova.runtime.repository

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.timestamp
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import java.math.BigInteger

typealias UnixTime = BigInteger

interface TimestampRepository {

    suspend fun now(chainId: ChainId): UnixTime
}

class RemoteTimestampRepository(
    private val remoteStorageDataSource: StorageDataSource
) : TimestampRepository {

    override suspend fun now(chainId: ChainId): UnixTime {
        return remoteStorageDataSource.query(chainId) {
            runtime.metadata.timestamp().storage("Now").query(binding = ::bindNumber)
        }
    }
}
