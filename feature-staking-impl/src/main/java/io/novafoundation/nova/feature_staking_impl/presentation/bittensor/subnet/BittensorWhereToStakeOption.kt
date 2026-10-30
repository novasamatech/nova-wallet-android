package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.feature_staking_impl.R

enum class BittensorWhereToStakeOption(
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    @DrawableRes val artwork: Int,
    @ColorRes val gradientStart: Int,
    @ColorRes val gradientMiddle: Int?,
    @ColorRes val gradientEnd: Int,
    @ColorRes val border: Int,
    val pros: List<Int>,
    val cons: List<Int>
) {
    POOL_SIZE(
        R.string.bittensor_quick_pick_pool,
        R.string.bittensor_quick_pool_order,
        R.drawable.bittensor_quick_pool,
        R.color.bittensor_quick_pool_start,
        R.color.bittensor_quick_pool_middle,
        R.color.bittensor_quick_pool_end,
        R.color.bittensor_quick_pool_border,
        listOf(R.string.bittensor_quick_pool_pro_one, R.string.bittensor_quick_pool_pro_two),
        listOf(R.string.bittensor_quick_pool_con_one, R.string.bittensor_quick_pool_con_two)
    ),
    AGE(
        R.string.bittensor_subnet_age,
        R.string.bittensor_quick_age_order,
        R.drawable.bittensor_quick_age,
        R.color.bittensor_quick_age_start,
        R.color.bittensor_quick_age_middle,
        R.color.bittensor_quick_age_end,
        R.color.bittensor_quick_age_border,
        listOf(R.string.bittensor_quick_age_pro),
        listOf(R.string.bittensor_quick_age_con_one, R.string.bittensor_quick_age_con_two)
    ),
    APY(
        R.string.bittensor_quick_apy,
        R.string.bittensor_quick_apy_order,
        R.drawable.bittensor_quick_apy,
        R.color.bittensor_quick_apy_start,
        null,
        R.color.bittensor_quick_apy_end,
        R.color.bittensor_quick_apy_border,
        listOf(R.string.bittensor_quick_apy_pro),
        listOf(R.string.bittensor_quick_apy_con_one, R.string.bittensor_quick_apy_con_two)
    ),
    ROOT(
        R.string.bittensor_quick_root,
        R.string.bittensor_quick_root_order,
        R.drawable.bittensor_quick_root,
        R.color.bittensor_quick_root_start,
        null,
        R.color.bittensor_quick_root_end,
        R.color.bittensor_quick_root_border,
        listOf(R.string.bittensor_quick_root_pro_one, R.string.bittensor_quick_root_pro_two),
        emptyList()
    )
}
