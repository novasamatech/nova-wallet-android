package io.novafoundation.nova.feature_crowdloan_api.data.repository

import io.novafoundation.nova.feature_crowdloan_api.data.network.blockhain.binding.FundInfo
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

suspend fun CrowdloanRepository.hasWonAuction(chainId: ChainId, fundInfo: FundInfo): Boolean {
    val paraId = fundInfo.paraId

    return getWinnerInfo(chainId, mapOf(paraId to fundInfo)).getValue(paraId)
}
