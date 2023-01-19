package io.novafoundation.nova.feature_dapp_impl.data.repository

import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.core_db.dao.BrowserHostSettingsDao
import io.novafoundation.nova.feature_dapp_api.data.model.BrowserHostSettings
import io.novafoundation.nova.feature_dapp_api.data.repository.BrowserHostSettingsRepository
import io.novafoundation.nova.feature_dapp_impl.data.mappers.mapBrowserHostSettingsFromLocal
import io.novafoundation.nova.feature_dapp_impl.data.mappers.mapBrowserHostSettingsToLocal

class RealBrowserHostSettingsRepository(
    private val browserHostSettingsDao: BrowserHostSettingsDao
) : BrowserHostSettingsRepository {
    override suspend fun getBrowserHostSettings(url: String): BrowserHostSettings? {
        val hostUrl = Urls.normalizeUrl(url)
        val localSettings = browserHostSettingsDao.getBrowserHostSettings(hostUrl)
        return localSettings?.let { mapBrowserHostSettingsFromLocal(it) }
    }

    override suspend fun saveBrowserHostSettings(settings: BrowserHostSettings) {
        val localSettings = mapBrowserHostSettingsToLocal(settings)
        browserHostSettingsDao.insertBrowserHostSettings(localSettings)
    }
}
