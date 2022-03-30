package io.novafoundation.nova.feature_dapp_impl.data.repository

import io.novafoundation.nova.common.utils.retryUntilDone
import io.novafoundation.nova.feature_dapp_api.data.model.DappMetadata
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_impl.data.mappers.mapDAppMetadataResponseToDAppMetadatas
import io.novafoundation.nova.feature_dapp_impl.data.network.metadata.DappMetadataApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

class InMemoryDAppMetadataRepository(
    private val dappMetadataApi: DappMetadataApi,
    private val remoteApiUrl: String
) : DAppMetadataRepository {

    private val dappMetadatasFlow = MutableSharedFlow<List<DappMetadata>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override suspend fun syncDAppMetadatas() {
        val response = retryUntilDone { dappMetadataApi.getParachainMetadata(remoteApiUrl) }
        val dappMetadatas = mapDAppMetadataResponseToDAppMetadatas(response)

        dappMetadatasFlow.emit(dappMetadatas)
    }

    override suspend fun getDAppMetadata(baseUrl: String): DappMetadata? {
        return dappMetadatasFlow.first().find { it.baseUrl == baseUrl }
    }

    override suspend fun getDAppMetadatas(): List<DappMetadata> {
        return dappMetadatasFlow.first()
    }

    override fun observeDAppMetadatas(): Flow<List<DappMetadata>> {
        return dappMetadatasFlow
    }
}
