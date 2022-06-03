package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding

import io.novafoundation.nova.feature_staking_impl.domain.model.Unbonding
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

data class UnbondingModel(
    val id: String,
    val status: Unbonding.Status,
    val amountModel: AmountModel
)
