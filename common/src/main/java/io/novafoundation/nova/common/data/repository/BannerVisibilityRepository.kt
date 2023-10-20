package io.novafoundation.nova.common.data.repository

import io.novafoundation.nova.common.data.storage.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

typealias BannerTag = String

interface BannerVisibilityRepository {

    fun shouldShowBannerFlow(tag: BannerTag): Flow<Boolean>

    suspend fun hideBanner(tag: BannerTag)

    suspend fun showBanner(tag: BannerTag)
}

private const val SHOW_BANNER_DEFAULT = true

internal class RealBannerVisibilityRepository(
    private val preferences: Preferences
) : BannerVisibilityRepository {

    override fun shouldShowBannerFlow(tag: BannerTag): Flow<Boolean> {
        return preferences.booleanFlow(prefsTag(tag), defaultValue = SHOW_BANNER_DEFAULT)
    }

    override suspend fun hideBanner(tag: BannerTag) = withContext(Dispatchers.IO) {
        preferences.putBoolean(prefsTag(tag), false)
    }

    override suspend fun showBanner(tag: BannerTag) = withContext(Dispatchers.IO) {
        preferences.putBoolean(prefsTag(tag), true)
    }

    private fun prefsTag(bannerTag: BannerTag): String {
        return "BannerVisibilityRepository.$bannerTag"
    }
}
