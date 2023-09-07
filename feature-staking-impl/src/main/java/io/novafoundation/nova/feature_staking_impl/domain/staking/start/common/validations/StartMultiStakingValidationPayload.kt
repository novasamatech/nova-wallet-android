package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations

import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.RecommendableMultiStakingSelection
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee
import java.math.BigInteger

data class StartMultiStakingValidationPayload(
    val recommendableSelection: RecommendableMultiStakingSelection,
    val asset: Asset,
    val fee: DecimalFee,
) {
    val selection = recommendableSelection.selection
}

fun StartMultiStakingValidationPayload.amountOf(planks: BigInteger) = asset.token.amountFromPlanks(planks)
