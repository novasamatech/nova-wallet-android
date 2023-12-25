package io.novafoundation.nova.feature_staking_impl.domain.validations.unbond

import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee
import java.math.BigDecimal

data class UnbondValidationPayload(
    val stash: StakingState.Stash,
    val fee: DecimalFee,
    val amount: BigDecimal,
    val asset: Asset,
)
