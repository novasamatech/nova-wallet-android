package io.novafoundation.nova.common.data.repository

import io.novafoundation.nova.common.data.model.AssetIconMode
import io.novafoundation.nova.common.data.storage.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface AssetsIconModeService {
    fun assetsIconModeFlow(): Flow<AssetIconMode>

    suspend fun setAssetsIconMode(assetsViewMode: AssetIconMode)

    fun getIconMode(): AssetIconMode
}

private const val PREFS_ASSETS_ICON_MODE = "PREFS_ASSETS_ICON_MODE"
private val ASSET_ICON_MODE_DEFAULT = AssetIconMode.COLORED

class RealAssetsIconModeService(
    private val preferences: Preferences
) : AssetsIconModeService {

    override fun assetsIconModeFlow(): Flow<AssetIconMode> {
        return preferences.stringFlow(PREFS_ASSETS_ICON_MODE)
            .map {
                it?.fromPrefsValue() ?: ASSET_ICON_MODE_DEFAULT
            }
    }

    override suspend fun setAssetsIconMode(assetsViewMode: AssetIconMode) = withContext(Dispatchers.IO) {
        preferences.putString(PREFS_ASSETS_ICON_MODE, assetsViewMode.toPrefsValue())
    }

    override fun getIconMode(): AssetIconMode {
        return preferences.getString(PREFS_ASSETS_ICON_MODE)?.fromPrefsValue() ?: ASSET_ICON_MODE_DEFAULT
    }

    private fun AssetIconMode.toPrefsValue(): String {
        return when (this) {
            AssetIconMode.COLORED -> "colored"
            AssetIconMode.WHITE -> "solid"
        }
    }

    private fun String.fromPrefsValue(): AssetIconMode? {
        return when (this) {
            "colored" -> AssetIconMode.COLORED
            "solid" -> AssetIconMode.WHITE
            else -> null
        }
    }
}
