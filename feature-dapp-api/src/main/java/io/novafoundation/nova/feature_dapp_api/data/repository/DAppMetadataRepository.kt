package io.novafoundation.nova.feature_dapp_api.data.repository

import io.novafoundation.nova.feature_dapp_api.data.model.DappCatalog
import io.novafoundation.nova.feature_dapp_api.data.model.DappMetadata
import kotlinx.coroutines.flow.Flow

interface DAppMetadataRepository {

    suspend fun syncDAppMetadatas()

    suspend fun syncAndGetDapp(baseUrl: String): DappMetadata?

    suspend fun getDAppMetadata(baseUrl: String): DappMetadata?

    suspend fun findDAppMetadataByExactUrlMatch(fullUrl: String): DappMetadata?

    suspend fun findDAppMetadatasByBaseUrlMatch(baseUrl: String): List<DappMetadata>

    suspend fun getDAppCatalog(): DappCatalog

    fun observeDAppCatalog(): Flow<DappCatalog>
}
