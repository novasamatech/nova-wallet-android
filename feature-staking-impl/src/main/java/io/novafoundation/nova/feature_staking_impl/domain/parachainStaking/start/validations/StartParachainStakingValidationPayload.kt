package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations

import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_account_api.data.model.Fee
import java.math.BigDecimal

class StartParachainStakingValidationPayload(
    val amount: BigDecimal,
    val fee: Fee,
    val collator: Collator,
    val asset: Asset,
    val delegatorState: DelegatorState,
)
