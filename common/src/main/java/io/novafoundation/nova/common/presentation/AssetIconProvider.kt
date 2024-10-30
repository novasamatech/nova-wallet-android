package io.novafoundation.nova.common.presentation

import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.data.model.AssetIconMode
import io.novafoundation.nova.common.data.repository.AssetsIconModeRepository
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.asIcon

interface AssetIconProvider {

    val fallbackIcon: Icon

    fun getAssetIcon(iconName: String?, fallback: Icon = fallbackIcon): Icon

    fun getWhiteAssetIcon(iconName: String?, fallback: Icon = fallbackIcon): Icon
}

class RealAssetIconProvider(
    private val assetsIconModeRepository: AssetsIconModeRepository,
    private val coloredBaseUrl: String,
    private val whiteBaseUrl: String
) : AssetIconProvider {

    override val fallbackIcon: Icon = R.drawable.ic_nova.asIcon()

    override fun getAssetIcon(iconName: String?, fallback: Icon): Icon {
        if (iconName == null) return fallback

        val iconUrl = when (assetsIconModeRepository.getIconMode()) {
            AssetIconMode.COLORED -> "$coloredBaseUrl/$iconName"
            AssetIconMode.WHITE -> "$whiteBaseUrl/$iconName"
        }

        return iconUrl.asIcon()
    }

    override fun getWhiteAssetIcon(iconName: String?, fallback: Icon): Icon {
        return if (iconName == null) {
            fallback
        } else {
            "$whiteBaseUrl/$iconName".asIcon()
        }
    }
}
