package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.flow.Flow

/**
 * Per-wallet show/hide state for assets.
 *
 * A row is a decision that has been made - by the user in Manage Tokens, or by the app noticing a
 * balance. No row means nobody has decided yet and the curated default list applies.
 */
interface AssetVisibilityRepository {

    fun visibilityFlow(metaId: Long): Flow<Map<FullChainAssetId, Boolean>>

    suspend fun getVisibility(metaId: Long): Map<FullChainAssetId, Boolean>

    /**
     * Writes decisions, replacing whatever was there. Used wherever something explicit happens -
     * a switch being toggled, or funds the user moved themselves arriving.
     */
    suspend fun setVisibility(metaId: Long, choices: Map<FullChainAssetId, Boolean>)

    /**
     * Shows assets that have no row yet, leaving existing rows alone. This is what keeps balance
     * discovery from undoing a token the user hid on purpose.
     */
    suspend fun showIfUndecided(metaId: Long, assetIds: Collection<FullChainAssetId>)
}
