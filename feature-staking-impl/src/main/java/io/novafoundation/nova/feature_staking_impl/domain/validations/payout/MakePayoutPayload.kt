package io.novafoundation.nova.feature_staking_impl.domain.validations.payout

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

class MakePayoutPayload(
    val originAddress: String,
    val fee: BigDecimal,
    val totalReward: BigDecimal,
    val chainAsset: Chain.Asset,
    val payoutStakersCalls: List<PayoutStakersPayload>
) {
    data class PayoutStakersPayload(val era: BigInteger, val validatorAddress: String)
}
