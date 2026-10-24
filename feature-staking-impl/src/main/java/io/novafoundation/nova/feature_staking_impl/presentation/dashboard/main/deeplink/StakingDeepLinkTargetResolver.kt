package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink

import io.novafoundation.nova.feature_deep_linking.presentation.handling.common.DeepLinkHandlingException.StakingHandlingException
import io.novafoundation.nova.runtime.ext.isEnabled
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getChainOrNull

class StakingDeepLinkTargetResolver(private val chainRegistry: ChainRegistry) {

    suspend fun resolve(chainId: String?, assetId: String?): StakingDeepLinkTarget? {
        if (chainId == null) return null
        if (chainId.isBlank()) throw StakingHandlingException.ChainIsNotFound
        val target = try {
            requireNotNull(parseStakingDeepLinkTarget(chainId, assetId))
        } catch (e: IllegalArgumentException) {
            throw StakingHandlingException.AssetIsNotFound
        }
        val chain = chainRegistry.getChainOrNull(target.chainId)?.takeIf { it.isEnabled }
            ?: throw StakingHandlingException.ChainIsNotFound
        val asset = chain.assetsById[target.assetId] ?: throw StakingHandlingException.AssetIsNotFound
        if (asset.staking.isEmpty()) throw StakingHandlingException.StakingIsNotSupported
        return StakingDeepLinkTarget(chain.id, asset.id)
    }
}
