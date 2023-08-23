package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations

import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee
import java.math.BigDecimal
import java.math.BigInteger

class StartMultiStakingValidationPayload(
    val selection: StartMultiStakingSelection,
    val asset: Asset,
    val fee: DecimalFee,
)

fun StartMultiStakingValidationPayload.planksOf(amount: BigDecimal) = asset.token.planksFromAmount(amount)
fun StartMultiStakingValidationPayload.amountOf(planks: BigInteger) = asset.token.amountFromPlanks(planks)
