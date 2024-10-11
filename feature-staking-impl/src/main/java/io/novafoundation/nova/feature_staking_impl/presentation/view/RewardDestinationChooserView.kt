package io.novafoundation.nova.feature_staking_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import io.novafoundation.nova.feature_account_api.view.AccountView
import io.novafoundation.nova.feature_staking_impl.R

class RewardDestinationChooserView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    init {
        orientation = VERTICAL

        View.inflate(context, R.layout.view_reward_destination_chooser, this)
    }

    val learnMore: TextView
        get() = rewardDestinationChooserLearnMore

    val destinationRestake: RewardDestinationView
        get() = rewardDestinationChooserRestake

    val destinationPayout: RewardDestinationView
        get() = rewardDestinationChooserPayout

    val payoutTarget: AccountView
        get() = rewardDestinationChooserPayoutTarget

    val payoutTitle: TextView
        get() = rewardDestinationChooserPayoutTitle
}
