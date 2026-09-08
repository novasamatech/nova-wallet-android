package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

/**
 * Reveals a token the user has just moved their own funds into.
 *
 * This deliberately overrides a remembered manual hide: the alternative is funds sitting in a token
 * the wallet refuses to show, which reads as lost money. Clearing the override rather than setting
 * one also hands the token back to the usual balance-driven rules afterwards.
 */
class ShowReceivedAssetUseCase(
    private val chainAssetRepository: ChainAssetRepository
) {

    suspend fun onOwnFundsReceived(assetId: FullChainAssetId) {
        chainAssetRepository.setAssetsEnabled(listOf(AssetEnabledUpdate.automatic(assetId, enabled = true)))
    }
}
