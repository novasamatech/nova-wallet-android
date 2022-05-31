package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond

import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.SelectedCollator
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class UnbondingCollator(
    collator: Collator,
    delegation: Balance,
    val hasPendingUnbonding: Boolean
) : SelectedCollator(collator, delegation)

fun UnbondingCollator(selectedCollator: SelectedCollator, hasPendingUnbonding: Boolean) = UnbondingCollator(
    collator = selectedCollator.collator,
    delegation = selectedCollator.delegation,
    hasPendingUnbonding = hasPendingUnbonding
)
