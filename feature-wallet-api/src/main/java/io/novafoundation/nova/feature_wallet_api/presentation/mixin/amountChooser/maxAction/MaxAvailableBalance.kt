package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

data class MaxAvailableBalance(val chainAsset: Chain.Asset, val displayedBalance: Balance, val actualBalance: Balance) {

    companion object {

        fun fromSingle(chainAsset: Chain.Asset, balance: Balance): MaxAvailableBalance {
            return MaxAvailableBalance(chainAsset, balance, balance)
        }
    }
}

val MaxAvailableBalance.actualAmount: BigDecimal
    get() = chainAsset.amountFromPlanks(actualBalance)
