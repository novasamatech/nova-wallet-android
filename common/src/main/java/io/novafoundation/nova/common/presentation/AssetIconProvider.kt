package io.novafoundation.nova.common.presentation

import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.data.model.AssetIconMode
import io.novafoundation.nova.common.data.repository.AssetsIconModeRepository
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.asIcon
import io.novafoundation.nova.common.utils.images.asUrlIcon

interface AssetIconProvider {

    companion object;

    fun getAssetIcon(iconName: String): Icon

    fun getAssetIcon(iconName: String, iconMode: AssetIconMode): Icon
}

class RealAssetIconProvider(
    private val assetsIconModeRepository: AssetsIconModeRepository,
    private val coloredBaseUrl: String,
    private val whiteBaseUrl: String
) : AssetIconProvider {

    override fun getAssetIcon(iconName: String): Icon {
        return getAssetIcon(iconName, assetsIconModeRepository.getIconMode())
    }

    override fun getAssetIcon(iconName: String, iconMode: AssetIconMode): Icon {
        val iconUrl = when (iconMode) {
            AssetIconMode.COLORED -> "$coloredBaseUrl/$iconName"
            AssetIconMode.WHITE -> "$whiteBaseUrl/$iconName"
        }

        return iconUrl.asUrlIcon()
    }
}

val AssetIconProvider.Companion.fallbackIcon: Icon
    get() = R.drawable.ic_nova.asIcon()

fun AssetIconProvider.getAssetIconOrFallback(
    iconName: String?,
    fallback: Icon = AssetIconProvider.fallbackIcon
): Icon {
    return iconName?.let { getAssetIcon(it) } ?: fallback
}

fun AssetIconProvider.getAssetIconOrFallback(
    iconName: String?,
    iconMode: AssetIconMode,
    fallback: Icon = AssetIconProvider.fallbackIcon
): Icon {
    return iconName?.let { getAssetIcon(it, iconMode) } ?: fallback
}
