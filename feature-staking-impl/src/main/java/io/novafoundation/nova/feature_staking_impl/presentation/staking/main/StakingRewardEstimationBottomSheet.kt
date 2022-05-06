package io.novafoundation.nova.feature_staking_impl.presentation.staking.main

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.android.synthetic.main.item_sheet_staking_reward_estimation.view.itemSheetStakingEstimateTitle
import kotlinx.android.synthetic.main.item_sheet_staking_reward_estimation.view.itemSheetStakingEstimateValue

class StakingRewardEstimationBottomSheet(
    context: Context,
    private val payload: Payload,
) : FixedListBottomSheet(context) {

    class Payload(
        val max: String,
        val average: String,
        @StringRes val returnsTypeFormat: Int,
        val title: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(payload.title)

        addItem(payload.max, payload.formatWithReturnType(R.string.common_maximum))
        addItem(payload.average, payload.formatWithReturnType(R.string.common_average))
    }

    private fun addItem(
        percentage: String,
        title: String,
    ) {
        item(R.layout.item_sheet_staking_reward_estimation) {
            it.itemSheetStakingEstimateTitle.text = title
            it.itemSheetStakingEstimateValue.text = percentage
        }
    }

    private fun Payload.formatWithReturnType(@StringRes content: Int): String {
        return context.getString(
            returnsTypeFormat,
            context.getString(content)
        )
    }
}
