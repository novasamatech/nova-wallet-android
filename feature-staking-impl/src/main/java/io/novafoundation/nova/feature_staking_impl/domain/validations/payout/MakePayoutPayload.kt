package io.novafoundation.nova.feature_staking_impl.domain.validations.payout

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal
import java.math.BigInteger

class MakePayoutPayload(
    val originAddress: String,
    val fee: BigDecimal,
    val totalReward: BigDecimal,
    val asset: Asset,
    val payoutStakersCalls: List<PayoutStakersPayload>
) {
    data class PayoutStakersPayload(val era: BigInteger, val validatorAddress: String)
}
