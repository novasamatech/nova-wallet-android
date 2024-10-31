package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.amount

import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_account_api.data.model.getAmount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class DefaultFeeBalanceExtractor<F : FeeBase> : FeeBalanceExtractor<F> {

    override fun requiredBalanceToPayFee(fee: F, chainAsset: Chain.Asset): Balance {
        return fee.getAmount(chainAsset)
    }
}
