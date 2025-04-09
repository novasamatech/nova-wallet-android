package io.novafoundation.nova.feature_wallet_api.data.repository

import io.novafoundation.nova.feature_xcm_api.chain.XcmChain
import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.chainLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.repository.ParachainInfoRepository

suspend fun ParachainInfoRepository.getXcmChain(chain: Chain): XcmChain {
    return XcmChain(paraId(chain.id), chain)
}

suspend fun ParachainInfoRepository.getChainLocation(chainId: ChainId): ChainLocation {
    val location = AbsoluteMultiLocation.chainLocation(paraId(chainId))
    return ChainLocation(chainId, location)
}
