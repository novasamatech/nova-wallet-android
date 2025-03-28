package io.novafoundation.nova.feature_wallet_api.data.network.crosschain

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class CrossChainFee(
    val metadata: CrossChainFeeMetadata,
    val byAccount: Balance?,
    val fromAmountByChain: Map<ChainId, Balance>
) {

    companion object;
}

typealias CrossChainFeeMetadata = Any?
