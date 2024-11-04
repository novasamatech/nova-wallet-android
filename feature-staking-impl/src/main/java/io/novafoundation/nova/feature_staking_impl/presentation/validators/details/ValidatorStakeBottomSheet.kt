package io.novafoundation.nova.feature_staking_impl.presentation.validators.details

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.annotation.StringRes
import androidx.core.view.updateMarginsRelative
import io.novafoundation.nova.common.databinding.BottomSheeetFixedListBinding
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.showAmount

class ValidatorStakeBottomSheet(
    context: Context,
    private val payload: Payload
) : FixedListBottomSheet<BottomSheeetFixedListBinding>(context, viewConfiguration = ViewConfiguration.default(context)),
    WithContextExtensions by WithContextExtensions(
        context
    ) {

    class Payload(
        val own: AmountModel,
        val stakers: AmountModel,
        val total: AmountModel,
        @StringRes val stakersLabel: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.staking_validator_total_stake)

        item(createCellView()) {
            it.setTitle(R.string.staking_validator_own_stake)
            it.showAmount(payload.own)
        }

        item(createCellView()) {
            it.setTitle(payload.stakersLabel)
            it.showAmount(payload.stakers)
        }

        item(createCellView()) {
            it.setTitle(R.string.common_total)
            it.showAmount(payload.total)
        }
    }

    private fun createCellView(): TableCellView {
        return TableCellView(context).also { view ->
            view.layoutParams = ViewGroup.MarginLayoutParams(MATCH_PARENT, WRAP_CONTENT).also { params ->
                params.updateMarginsRelative(start = 16.dp, end = 16.dp)
            }
        }
    }
}
