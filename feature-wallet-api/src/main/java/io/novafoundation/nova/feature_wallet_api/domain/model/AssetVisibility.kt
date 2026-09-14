package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

/**
 * What one wallet shows, for one snapshot of the curated config and that wallet's own rows.
 *
 * Visibility is stored, not derived: a balance arriving on an asset writes a row instead of being
 * consulted here, so this answers from the row alone. With no row the curated list decides.
 */
class AssetVisibility(
    private val defaults: Set<FullChainAssetId>,
    private val userChoices: Map<FullChainAssetId, Boolean>
) {

    fun isVisible(asset: Asset): Boolean = isVisible(asset.token.configuration.fullId)

    fun isVisible(assetId: FullChainAssetId): Boolean {
        userChoices[assetId]?.let { return it }

        // An unreadable config leaves an empty list, which must never be read as "hide everything" -
        // with no curation to apply, everything stays visible.
        return defaults.isEmpty() || assetId in defaults
    }
}

fun List<Asset>.onlyVisible(visibility: AssetVisibility): List<Asset> = filter(visibility::isVisible)
