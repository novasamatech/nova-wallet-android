package io.novafoundation.nova.common.data.model

enum class AssetViewMode {
    TOKENS, NETWORKS
}

fun AssetViewMode.switch(): AssetViewMode {
    return when (this) {
        AssetViewMode.TOKENS -> AssetViewMode.NETWORKS
        AssetViewMode.NETWORKS -> AssetViewMode.TOKENS
    }
}
