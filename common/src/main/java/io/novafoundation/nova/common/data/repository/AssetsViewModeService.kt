package io.novafoundation.nova.common.data.repository

import io.novafoundation.nova.common.data.model.AssetViewMode
import io.novafoundation.nova.common.data.storage.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface AssetsViewModeRepository {

    fun getAssetViewMode(): AssetViewMode

    fun assetsViewModeFlow(): Flow<AssetViewMode>

    suspend fun setAssetsViewMode(assetsViewMode: AssetViewMode)
}

private const val PREFS_ASSETS_VIEW_MODE = "PREFS_ASSETS_VIEW_MODE"
private val ASSET_VIEW_MODE_DEFAULT = AssetViewMode.TOKENS

class RealAssetsViewModeRepository(
    private val preferences: Preferences
) : AssetsViewModeRepository {

    override fun getAssetViewMode(): AssetViewMode {
        return preferences.getString(PREFS_ASSETS_VIEW_MODE)?.fromPrefsValue() ?: ASSET_VIEW_MODE_DEFAULT
    }

    override fun assetsViewModeFlow(): Flow<AssetViewMode> {
        return preferences.stringFlow(PREFS_ASSETS_VIEW_MODE)
            .map {
                it?.fromPrefsValue() ?: ASSET_VIEW_MODE_DEFAULT
            }
    }

    override suspend fun setAssetsViewMode(assetsViewMode: AssetViewMode) = withContext(Dispatchers.IO) {
        preferences.putString(PREFS_ASSETS_VIEW_MODE, assetsViewMode.toPrefsValue())
    }

    private fun AssetViewMode.toPrefsValue(): String {
        return when (this) {
            AssetViewMode.NETWORKS -> "networks"
            AssetViewMode.TOKENS -> "tokens"
        }
    }

    private fun String.fromPrefsValue(): AssetViewMode? {
        return when (this) {
            "networks" -> AssetViewMode.NETWORKS
            "tokens" -> AssetViewMode.TOKENS
            else -> null
        }
    }
}
