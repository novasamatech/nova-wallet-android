package io.novafoundation.nova.feature_wallet_api.presentation.validation

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

fun handleInsufficientBalanceCommission(asset: Chain.Asset, resourceManager: ResourceManager): TitleAndMessage {
    return resourceManager.getString(R.string.common_not_enough_funds_title) to
        resourceManager.getString(R.string.wallet_send_insufficient_balance_commission, asset.symbol)
}
