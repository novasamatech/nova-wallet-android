package io.novafoundation.nova.feature_staking_impl.presentation.staking.main

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.android.synthetic.main.item_sheet_staking_action.view.itemSheetStakingActionImage
import kotlinx.android.synthetic.main.item_sheet_staking_action.view.itemSheetStakingActionText

class ManageStakingBottomSheet(
    context: Context,
    private val payload: Payload,
    private val onItemChosen: (ManageStakeAction) -> Unit,
) : FixedListBottomSheet(context) {

    class Payload(val availableActions: Set<ManageStakeAction>)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.staking_manage_title)

        manageItem(R.drawable.ic_basic_layers_24, R.string.staking_balance_title_v2_2_0, ManageStakeAction.BALANCE)
//        manageItem(R.drawable.ic_stop_circle_24, R.string.staking_pause_staking)
//        manageItem(R.drawable.ic_send, R.string.staking_unstake)
//        manageItem(R.drawable.ic_dotted_list_24, R.string.staking_unstaking_requests)
        manageItem(R.drawable.ic_pending_reward, R.string.staking_reward_payouts_title_v2_2_0, ManageStakeAction.PAYOUTS)
        manageItem(R.drawable.ic_finance_wallet_24, R.string.staking_rewards_destination_title, ManageStakeAction.REWARD_DESTINATION)
        manageItem(R.drawable.ic_validators_outline, R.string.staking_your_validators, ManageStakeAction.VALIDATORS)

        manageItem(R.drawable.ic_people_outline, R.string.staking_controller_account, ManageStakeAction.CONTROLLER)
    }

    private inline fun manageItem(
        @DrawableRes iconRes: Int,
        @StringRes titleRes: Int,
        action: ManageStakeAction,
        crossinline extraBuilder: (View) -> Unit = {},
    ) {
        if (action in payload.availableActions) {
            item(R.layout.item_sheet_staking_action) {
                it.itemSheetStakingActionImage.setImageResource(iconRes)
                it.itemSheetStakingActionText.setText(titleRes)

                extraBuilder(it)

                it.setDismissingClickListener {
                    onItemChosen(action)
                }
            }
        }
    }
}
