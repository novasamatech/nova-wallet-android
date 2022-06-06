package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.current.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.feature_staking_impl.R

enum class ManageCollatorsAction(@StringRes val titleRes: Int, @DrawableRes val iconRes: Int) {
    BOND_MORE(R.string.staking_bond_more_v1_9_0, R.drawable.ic_plus_circle), UNBOND(R.string.staking_unbond_v1_9_0, R.drawable.ic_unstake_24)
}
