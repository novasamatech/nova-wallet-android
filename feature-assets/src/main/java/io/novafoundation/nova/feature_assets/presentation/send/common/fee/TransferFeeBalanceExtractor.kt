package io.novafoundation.nova.feature_assets.presentation.send.common.fee

import io.novafoundation.nova.feature_assets.domain.send.model.TransferFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.amount.FeeBalanceExtractor
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class TransferFeeBalanceExtractor : FeeBalanceExtractor<TransferFee> {

    override fun requiredBalanceToPayFee(fee: TransferFee, chainAsset: Chain.Asset): Balance {
        return fee.totalFeeByExecutingAccount(chainAsset)
    }
}
