package io.novafoundation.nova.feature_swap_impl.data.assetExchange.xcm

import io.novafoundation.nova.feature_wallet_api.data.repository.getXcmChain
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.TokenReserveRegistry
import io.novafoundation.nova.feature_xcm_api.chain.chainLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.AssetLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.repository.ParachainInfoRepository

interface XcmLocationConverter {

    suspend fun getChainLocation(chainId: ChainId): ChainLocation

    suspend fun getAssetLocation(chainAssetId: FullChainAssetId): AssetLocation

    suspend fun canConvertAssetLocation(chainAssetId: FullChainAssetId): Boolean
}

class RealXcmLocationConverter(
    private val tokenReserveRegistry: TokenReserveRegistry,
    private val chainRegistry: ChainRegistry,
    private val parachainRepository: ParachainInfoRepository
): XcmLocationConverter {

    override suspend fun getChainLocation(chainId: ChainId): ChainLocation {
        val chain = chainRegistry.getChain(chainId)
        val xcmChain = parachainRepository.getXcmChain(chain)

        return xcmChain.chainLocation()
    }

    override suspend fun getAssetLocation(chainAssetId: FullChainAssetId): AssetLocation {
        val chainAsset = chainRegistry.asset(chainAssetId)
        val reserve = tokenReserveRegistry.getReserve(chainAsset)

        return AssetLocation(chainAssetId, reserve.location)
    }

    override suspend fun canConvertAssetLocation(chainAssetId: FullChainAssetId): Boolean {
        val chainAsset = chainRegistry.asset(chainAssetId)
        return tokenReserveRegistry.isReserveKnown(chainAsset)
    }
}
