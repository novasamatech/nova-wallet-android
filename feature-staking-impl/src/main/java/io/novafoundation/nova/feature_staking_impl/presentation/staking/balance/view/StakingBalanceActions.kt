package io.novafoundation.nova.feature_staking_impl.presentation.staking.balance.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.common.view.shape.getBlurDrawable
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.android.synthetic.main.view_staking_balance_actions.view.stakingBalanceActionsBondMore
import kotlinx.android.synthetic.main.view_staking_balance_actions.view.stakingBalanceActionsRedeem
import kotlinx.android.synthetic.main.view_staking_balance_actions.view.stakingBalanceActionsUnbond

class StakingBalanceActions
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    init {
        orientation = HORIZONTAL

        View.inflate(context, R.layout.view_staking_balance_actions, this)

        background = context.getBlurDrawable()

        updatePadding(top = 4.dp(context), bottom = 4.dp(context))
    }

    val bondMore: TextView
        get() = stakingBalanceActionsBondMore

    val unbond: TextView
        get() = stakingBalanceActionsUnbond

    val redeem: TextView
        get() = stakingBalanceActionsRedeem
}
