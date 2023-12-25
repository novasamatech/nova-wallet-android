package io.novafoundation.nova.feature_staking_impl.domain.validations.payout

import io.novafoundation.nova.feature_staking_impl.data.model.Payout
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee
import java.math.BigDecimal

class MakePayoutPayload(
    val originAddress: String,
    val fee: DecimalFee,
    val totalReward: BigDecimal,
    val asset: Asset,
    val payouts: List<Payout>
)
