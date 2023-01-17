package io.novafoundation.nova.feature_dapp_api.data.repository

import io.novafoundation.nova.feature_dapp_api.data.model.BrowserHostSettings

interface BrowserHostSettingsRepository {

    suspend fun getBrowserHostSettings(url: String): BrowserHostSettings?

    suspend fun saveBrowserHostSettings(settings: BrowserHostSettings)
}
