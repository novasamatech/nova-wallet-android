package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy

class BalanceMaxActionProvider(
    private val chainAssetFlow: Flow<Chain.Asset>,
    private val balanceFlow: Flow<Balance>,
) : MaxActionProvider {

    override val maxAvailableBalance: Flow<MaxAvailableBalance> = combine(
        chainAssetFlow.distinctUntilChangedBy { it.fullId },
        balanceFlow
    ) { chainAsset, balance ->
        MaxAvailableBalance.fromSingle(chainAsset, balance)
    }
}
