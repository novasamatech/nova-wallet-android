package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.amount

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface FeeInspector<F> {

    fun requiredBalanceToPayFee(fee: F, chainAsset: Chain.Asset): Balance

    fun getSubmissionFeeAsset(fee: F): Chain.Asset
}
