package io.novafoundation.nova.feature_staking_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_account_api.view.AccountView
import io.novafoundation.nova.feature_staking_impl.databinding.ViewRewardDestinationChooserBinding

class RewardDestinationChooserView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    private val binder = ViewRewardDestinationChooserBinding.inflate(inflater(), this)

    init {
        orientation = VERTICAL
    }

    val learnMore: TextView
        get() = binder.rewardDestinationChooserLearnMore

    val destinationRestake: RewardDestinationView
        get() = binder.rewardDestinationChooserRestake

    val destinationPayout: RewardDestinationView
        get() = binder.rewardDestinationChooserPayout

    val payoutTarget: AccountView
        get() = binder.rewardDestinationChooserPayoutTarget

    val payoutTitle: TextView
        get() = binder.rewardDestinationChooserPayoutTitle
}
