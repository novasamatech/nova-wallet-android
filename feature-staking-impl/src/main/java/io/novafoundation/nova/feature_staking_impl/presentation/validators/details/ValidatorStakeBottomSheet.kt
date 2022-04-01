package io.novafoundation.nova.feature_staking_impl.presentation.validators.details

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.view.updateMarginsRelative
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount

class ValidatorStakeBottomSheet(
    context: Context,
    private val payload: Payload
) : FixedListBottomSheet(context), WithContextExtensions by WithContextExtensions(context) {

    class Payload(
        val own: AmountModel,
        val nominators: AmountModel,
        val total: AmountModel,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.staking_validator_total_stake)
        setTitleDividerVisible(false)

        item(createCellView()) {
            it.setTitle(R.string.staking_validator_own_stake)
            it.showAmount(payload.own)
        }

        item(createCellView()) {
            it.setTitle(R.string.staking_validator_nominators)
            it.showAmount(payload.nominators)
        }

        item(createCellView()) {
            it.setTitle(R.string.wallet_send_total_title)
            it.showAmount(payload.total)
        }
    }

    private fun createCellView(): TableCellView {
        return TableCellView(context).also { view ->
            view.layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT).also { params ->
                params.updateMarginsRelative(start = 16.dp, end = 16.dp)
            }

            view.setDividerColor(R.color.white_8)
        }
    }
}
