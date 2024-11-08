package io.novafoundation.nova.common.presentation

import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.data.model.AssetIconMode
import io.novafoundation.nova.common.data.repository.AssetsIconModeRepository
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.asIcon

interface AssetIconProvider {

    companion object;

    fun getAssetIconOrFallback(iconName: String): Icon

    fun getAssetIconOrFallback(iconName: String, iconMode: AssetIconMode): Icon
}

class RealAssetIconProvider(
    private val assetsIconModeRepository: AssetsIconModeRepository,
    private val coloredBaseUrl: String,
    private val whiteBaseUrl: String
) : AssetIconProvider {

    override fun getAssetIconOrFallback(iconName: String): Icon {
        return getAssetIconOrFallback(iconName, assetsIconModeRepository.getIconMode())
    }

    override fun getAssetIconOrFallback(iconName: String, iconMode: AssetIconMode): Icon {
        val iconUrl = when (iconMode) {
            AssetIconMode.COLORED -> "$coloredBaseUrl/$iconName"
            AssetIconMode.WHITE -> "$whiteBaseUrl/$iconName"
        }

        return iconUrl.asIcon()
    }
}

val AssetIconProvider.Companion.fallbackIcon: Icon
    get() = R.drawable.ic_nova.asIcon()

fun AssetIconProvider.getAssetIconOrFallback(
    iconName: String?,
    fallback: Icon = AssetIconProvider.fallbackIcon
): Icon {
    return iconName?.let { getAssetIconOrFallback(it) } ?: fallback
}

fun AssetIconProvider.getAssetIconOrFallback(
    iconName: String?,
    iconMode: AssetIconMode,
    fallback: Icon = AssetIconProvider.fallbackIcon
): Icon {
    return iconName?.let { getAssetIconOrFallback(it, iconMode) } ?: fallback
}
