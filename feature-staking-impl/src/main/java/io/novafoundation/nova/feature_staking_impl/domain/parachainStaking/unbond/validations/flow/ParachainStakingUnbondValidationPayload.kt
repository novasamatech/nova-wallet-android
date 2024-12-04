package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow

import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_account_api.data.model.Fee
import java.math.BigDecimal

data class ParachainStakingUnbondValidationPayload(
    val amount: BigDecimal,
    val fee: Fee,
    val collator: Collator,
    val asset: Asset,
)
