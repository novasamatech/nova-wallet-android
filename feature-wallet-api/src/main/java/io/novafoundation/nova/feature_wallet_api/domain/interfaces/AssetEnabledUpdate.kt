package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

/**
 * A change to whether one asset is shown in the wallet.
 *
 * [overriddenByUser] records that this state was chosen by the user *against* what automatic
 * enabling by balance would have done, which is the only case worth remembering: a choice that
 * agrees with the automatic rule needs no protection from it.
 */
class AssetEnabledUpdate(
    val assetId: FullChainAssetId,
    val enabled: Boolean,
    val overriddenByUser: Boolean
) {

    companion object {

        /**
         * Enabling done by the app itself, which the user stays free to undo.
         */
        fun automatic(assetId: FullChainAssetId, enabled: Boolean) = AssetEnabledUpdate(assetId, enabled, overriddenByUser = false)

        /**
         * Enabling chosen by the user. The choice is only remembered when it contradicts the
         * automatic rule for the balance the asset has right now.
         */
        fun byUser(assetId: FullChainAssetId, enabled: Boolean, hasPositiveBalance: Boolean): AssetEnabledUpdate {
            return AssetEnabledUpdate(assetId, enabled, overriddenByUser = enabled != hasPositiveBalance)
        }
    }
}
