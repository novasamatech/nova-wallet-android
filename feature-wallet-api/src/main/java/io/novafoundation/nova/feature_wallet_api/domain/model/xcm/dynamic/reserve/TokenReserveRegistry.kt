package io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve

import io.novafoundation.nova.feature_xcm_api.config.model.AssetsXcmConfig
import io.novafoundation.nova.feature_xcm_api.config.model.getReserve
import io.novafoundation.nova.feature_xcm_api.converter.chain.ChainLocationConverter
import io.novafoundation.nova.feature_xcm_api.converter.chain.chainLocationOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class TokenReserveRegistry(
    private val xcmConfig: AssetsXcmConfig,
    private val chainLocationConverter: ChainLocationConverter,
) {

    suspend fun getReserve(chainAsset: Chain.Asset): TokenReserve {
        val reserve = xcmConfig.getReserve(chainAsset)
        return TokenReserve(
            reserveChainLocation = chainLocationConverter.chainLocationOf(reserve.reserveId),
            tokenLocation = reserve.tokenLocation
        )
    }
}
