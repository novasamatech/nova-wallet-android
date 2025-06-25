package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash

data class TransferableBalanceUpdatePoint(
    val updatedAt: BlockHash
)
