package io.novafoundation.nova.feature_dapp_impl.data.mappers

import io.novafoundation.nova.core_db.model.BrowserHostSettingsLocal
import io.novafoundation.nova.feature_dapp_api.data.model.BrowserHostSettings

fun mapBrowserHostSettingsToLocal(settings: BrowserHostSettings): BrowserHostSettingsLocal {
    return BrowserHostSettingsLocal(
        settings.hostUrl,
        settings.isDesktopModeEnabled
    )
}

fun mapBrowserHostSettingsFromLocal(settings: BrowserHostSettingsLocal): BrowserHostSettings {
    return BrowserHostSettings(
        settings.hostUrl,
        settings.isDesktopModeEnabled
    )
}
