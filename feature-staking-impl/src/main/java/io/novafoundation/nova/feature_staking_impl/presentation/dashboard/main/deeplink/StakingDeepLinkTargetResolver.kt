package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink

import io.novafoundation.nova.feature_deep_linking.presentation.handling.common.DeepLinkHandlingException.StakingHandlingException
import io.novafoundation.nova.runtime.ext.isEnabled
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.getChainOrNull

private const val DEFAULT_ASSET_ID = 0

/**
 * Turns the optional `chainId` / `assetId` query of the staking deep link into the asset to open.
 * No `chainId` means the plain dashboard; `assetId` defaults to the chain's native asset.
 */
class StakingDeepLinkTargetResolver(
    private val chainRegistry: ChainRegistry
) {

    suspend fun resolve(chainIdParam: String?, assetIdParam: String?): FullChainAssetId? {
        val chainId = chainIdParam?.takeIf { it.isNotBlank() } ?: return null

        val assetId = assetIdParam?.takeIf { it.isNotBlank() }
            ?.let { it.toIntOrNull() ?: throw StakingHandlingException.AssetIsNotFound }
            ?: DEFAULT_ASSET_ID

        val chain = chainRegistry.getChainOrNull(chainId)
            ?.takeIf { it.isEnabled }
            ?: throw StakingHandlingException.ChainIsNotFound

        val asset = chain.assetsById[assetId] ?: throw StakingHandlingException.AssetIsNotFound

        if (asset.staking.isEmpty()) throw StakingHandlingException.StakingIsNotSupported

        return FullChainAssetId(chain.id, asset.id)
    }
}
