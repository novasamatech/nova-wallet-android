package io.novafoundation.nova.runtime.multiNetwork.chain

import io.novafoundation.nova.core_db.dao.FullAssetIdLocal

/**
 * Decides whether an asset that has no local state yet starts out visible.
 *
 * Assets already present locally are never routed through here - their [ChainAssetLocal.enabled]
 * is preserved as-is, so nothing changes for a wallet that is already in use.
 */
sealed class InitialAssetEnabling {

    abstract fun isEnabled(assetId: FullAssetIdLocal): Boolean

    /**
     * A fresh install: only the curated default list starts visible, everything else is hidden
     * until the user adds it or it is picked up by its balance.
     */
    class ApplyDefaults(private val defaults: Set<FullAssetIdLocal>) : InitialAssetEnabling() {

        override fun isEnabled(assetId: FullAssetIdLocal) = assetId in defaults
    }

    /**
     * Defaults were already applied on an earlier launch, so an asset appearing now is new to an
     * existing wallet and must not add itself to that user's list.
     */
    object AlreadyApplied : InitialAssetEnabling() {

        override fun isEnabled(assetId: FullAssetIdLocal) = false
    }
}
