package io.novafoundation.nova.feature_dapp_api.data.repository

import io.novafoundation.nova.feature_dapp_api.data.model.DappMetadata
import kotlinx.coroutines.flow.Flow

interface DAppMetadataRepository {

    suspend fun syncDAppMetadatas()

    suspend fun getDAppMetadata(baseUrl: String): DappMetadata?

    suspend fun findDAppMetadataByExactUrlMatch(fullUrl: String): DappMetadata?

    suspend fun getDAppMetadatas(): List<DappMetadata>

    fun observeDAppMetadatas(): Flow<List<DappMetadata>>
}
