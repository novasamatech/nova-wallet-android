package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding

import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_staking_impl.domain.model.Unbonding
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.Unbondings
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel

fun UnbondingState.Companion.from(
    unbondings: Unbondings,
    asset: Asset,
    cancelLoading: Boolean = false
): UnbondingState {
    return when {
        unbondings.unbondings.isEmpty() -> UnbondingState.Empty
        else -> {
            UnbondingState.HaveUnbondings(
                redeemEnabled = unbondings.anythingToRedeem,
                cancelState = when {
                    cancelLoading -> ButtonState.PROGRESS
                    unbondings.anythingToUnbond -> ButtonState.NORMAL
                    else -> ButtonState.DISABLED
                },
                unbondings = unbondings.unbondings.mapIndexed { idx, unbonding ->
                    mapUnbondingToUnbondingModel(idx, unbonding, asset)
                }
            )
        }
    }
}

private fun mapUnbondingToUnbondingModel(index: Int, unbonding: Unbonding, asset: Asset): UnbondingModel {
    return UnbondingModel(
        index = index,
        status = unbonding.status,
        amountModel = mapAmountToAmountModel(unbonding.amount, asset)
    )
}
