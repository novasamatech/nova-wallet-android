package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction

import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChainAssetWithAmountMaxActionProvider(
    private val balance: Flow<ChainAssetWithAmount>
) : MaxActionProvider {

    override val maxAvailableBalance: Flow<MaxAvailableBalance> = balance.map {
        MaxAvailableBalance.fromSingle(it.chainAsset, it.amount)
    }
}
