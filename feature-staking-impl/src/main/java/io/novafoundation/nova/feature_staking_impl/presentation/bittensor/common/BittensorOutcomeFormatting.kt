package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationOutcome
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorFeeUi
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

fun formatBittensorNativeResult(actual: BigInteger?, estimate: BigInteger, chain: Chain, formatter: TokenFormatter): AmountModel {
    val asset = chain.utilityAsset
    val amount = formatter.formatToken(BigDecimal(actual ?: estimate, asset.precision.value), asset.symbol)
    return AmountModel(if (actual == null) "≈ $amount" else amount, null)
}

fun BittensorOperationOutcome?.formatNetworkFee(
    estimate: Fee,
    chain: Chain,
    resources: ResourceManager,
    formatter: TokenFormatter
): BittensorFeeUi {
    // Alpha-paid fees carry their native equivalent in the event's taoAmount field.
    val actual = this?.takeIf { it.feePayer != null && it.networkFee != null }
    if (actual == null) {
        return BittensorFeeUi("≈ " + formatter.formatToken(estimate.decimalAmount, estimate.asset.symbol), null)
    }
    val amount = formatter.formatToken(BigDecimal(requireNotNull(actual.networkFee), chain.utilityAsset.precision.value), chain.utilityAsset.symbol)
    val payer = actual.feePayer
    val payerLabel = payer?.takeIf { it != actual.coldkey }
        ?.let { resources.getString(R.string.bittensor_result_paid_by, chain.addressOf(it.value)) }
    return BittensorFeeUi(amount.toString(), payerLabel)
}
