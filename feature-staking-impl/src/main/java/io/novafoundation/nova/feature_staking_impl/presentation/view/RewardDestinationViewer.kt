package io.novafoundation.nova.feature_staking_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.common.rewardDestination.RewardDestinationModel
import kotlinx.android.synthetic.main.view_reward_destination_viewer.view.viewRewardDestinationDestination
import kotlinx.android.synthetic.main.view_reward_destination_viewer.view.viewRewardDestinationPayoutAccount

class RewardDestinationViewer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    init {
        orientation = VERTICAL

        View.inflate(context, R.layout.view_reward_destination_viewer, this)
    }

    fun showRewardDestination(rewardDestinationModel: RewardDestinationModel) {
        viewRewardDestinationPayoutAccount.setVisible(rewardDestinationModel is RewardDestinationModel.Payout)
        viewRewardDestinationDestination.setDividerVisible(rewardDestinationModel is RewardDestinationModel.Restake)

        when (rewardDestinationModel) {
            is RewardDestinationModel.Restake -> {
                viewRewardDestinationDestination.showValue(context.getString(R.string.staking_setup_restake_v2_2_0))
            }
            is RewardDestinationModel.Payout -> {
                viewRewardDestinationDestination.showValue(context.getString(R.string.staking_payout_v2_2_0))
                viewRewardDestinationPayoutAccount.setMessage(rewardDestinationModel.destination.nameOrAddress)
                viewRewardDestinationPayoutAccount.setTextIcon(rewardDestinationModel.destination.image)
            }
        }
    }

    fun setPayoutAccountClickListener(listener: (View) -> Unit) {
        viewRewardDestinationPayoutAccount.setWholeClickListener(listener)
    }
}
