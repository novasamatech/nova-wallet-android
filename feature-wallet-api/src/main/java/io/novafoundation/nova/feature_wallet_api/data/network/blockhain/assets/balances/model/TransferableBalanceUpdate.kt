package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

data class TransferableBalanceUpdate(
    val newBalance: Balance,
    val updatedAt: BlockHash?
)
