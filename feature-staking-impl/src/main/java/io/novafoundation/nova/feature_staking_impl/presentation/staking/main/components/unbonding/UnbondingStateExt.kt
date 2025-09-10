package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding

import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_staking_impl.domain.model.Unbonding
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.Unbondings
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.Unbondings.RebondState.CAN_REBOND
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.Unbondings.RebondState.REBOND_NOT_POSSIBLE
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel

fun UnbondingState.Companion.from(
    unbondings: Unbondings,
    asset: Asset,
    amountFormatter: AmountFormatter,
    cancelLoading: Boolean = false
): UnbondingState {
    return when {
        unbondings.unbondings.isEmpty() -> UnbondingState.Empty
        else -> {
            UnbondingState.HaveUnbondings(
                redeemEnabled = unbondings.anythingToRedeem,
                cancelState = when {
                    cancelLoading -> ButtonState.PROGRESS
                    unbondings.rebondState == CAN_REBOND -> ButtonState.NORMAL
                    unbondings.rebondState == REBOND_NOT_POSSIBLE -> ButtonState.GONE
                    else -> ButtonState.DISABLED
                },
                unbondings = unbondings.unbondings.map { unbonding ->
                    mapUnbondingToUnbondingModel(unbonding, asset, amountFormatter)
                }
            )
        }
    }
}

private fun mapUnbondingToUnbondingModel(unbonding: Unbonding, asset: Asset, amountFormatter: AmountFormatter): UnbondingModel {
    return UnbondingModel(
        id = unbonding.id,
        status = unbonding.status,
        amountModel = amountFormatter.formatAmountToAmountModel(unbonding.amount, asset)
    )
}
