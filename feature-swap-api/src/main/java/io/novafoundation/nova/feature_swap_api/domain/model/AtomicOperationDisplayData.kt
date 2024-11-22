package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetIdWithAmount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

sealed class AtomicOperationDisplayData {

    data class Transfer(
        val from: FullChainAssetId,
        val to: FullChainAssetId,
        val amount: Balance
    ) : AtomicOperationDisplayData()

    data class Swap(
        val from: ChainAssetIdWithAmount,
        val to: ChainAssetIdWithAmount,
    ) : AtomicOperationDisplayData()
}
