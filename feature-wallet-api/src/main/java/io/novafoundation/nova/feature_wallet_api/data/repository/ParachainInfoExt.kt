package io.novafoundation.nova.feature_wallet_api.data.repository

import io.novafoundation.nova.feature_xcm_api.chain.XcmChain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ParachainInfoRepository

suspend fun ParachainInfoRepository.getXcmChain(chain: Chain): XcmChain {
    return XcmChain(paraId(chain.id), chain)
}
