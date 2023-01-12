package io.novafoundation.nova.feature_dapp_impl.data.repository

import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.common.utils.retryUntilDone
import io.novafoundation.nova.core_db.dao.BrowserHostSettingsDao
import io.novafoundation.nova.core_db.model.BrowserHostSettingsLocal
import io.novafoundation.nova.feature_dapp_api.data.model.DappMetadata
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_impl.data.mappers.mapDAppMetadataResponseToDAppMetadatas
import io.novafoundation.nova.feature_dapp_impl.data.network.metadata.DappMetadataApi
import io.novafoundation.nova.feature_dapp_impl.data.network.metadata.DappMetadataRemote
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

class RealDAppMetadataRepository(
    private val dappMetadataApi: DappMetadataApi,
    private val remoteApiUrl: String,
    private val browserHostSettingsDao: BrowserHostSettingsDao
) : DAppMetadataRepository {

    private val dappMetadatasFlow = MutableSharedFlow<List<DappMetadata>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override suspend fun syncDAppMetadatas() {
        val response = retryUntilDone { dappMetadataApi.getParachainMetadata(remoteApiUrl) }
        val dappMetadatas = mapDAppMetadataResponseToDAppMetadatas(response)
        syncHostSettings(response.dapps)
        dappMetadatasFlow.emit(dappMetadatas)
    }

    override suspend fun getDAppMetadata(baseUrl: String): DappMetadata? {
        return dappMetadatasFlow.first().find { it.baseUrl == baseUrl }
    }

    override suspend fun findDAppMetadataByExactUrlMatch(fullUrl: String): DappMetadata? {
        return dappMetadatasFlow.first().find { it.url == fullUrl }
    }

    override suspend fun findDAppMetadatasByBaseUrlMatch(baseUrl: String): List<DappMetadata> {
        return dappMetadatasFlow.first().filter { it.baseUrl == baseUrl }
    }

    override suspend fun getDAppMetadatas(): List<DappMetadata> {
        return dappMetadatasFlow.first()
    }

    override fun observeDAppMetadatas(): Flow<List<DappMetadata>> {
        return dappMetadatasFlow
    }

    private suspend fun syncHostSettings(dappMetadatas: List<DappMetadataRemote>) {
        val oldSettings = browserHostSettingsDao.getBrowserAllHostSettings()
        val newSettings = dappMetadatas
            .filter { it.desktopOnly != null }
            .map { BrowserHostSettingsLocal(Urls.normalizeUrl(it.url), it.desktopOnly!!) }
        val differ = CollectionDiffer.findDiff(newSettings, oldSettings, false)
        browserHostSettingsDao.insertBrowserHostSettings(differ.added)
    }
}
