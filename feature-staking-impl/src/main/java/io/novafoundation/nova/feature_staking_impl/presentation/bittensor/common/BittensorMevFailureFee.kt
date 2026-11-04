package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevPaidFee
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.mevPaidFee
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorFeeUi
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

fun BittensorOperationResult.Failure.formatMevFailureFee(estimate: Fee, chain: Chain, formatter: TokenFormatter): BittensorFeeUi? {
    val paid = mevPaidFee(estimate) ?: return null
    val token = chain.utilityAsset
    val amount = formatter.formatToken(BigDecimal(paid.amount, token.precision.value), token.symbol)
    return BittensorFeeUi((if (paid is BittensorMevPaidFee.Estimated) "≈ " else "") + amount, null)
}
