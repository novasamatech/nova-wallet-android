package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.model.AmountChangeModel.DifferenceModel

class AmountChangeModel(
    val to: CharSequence,
    val from: CharSequence?,
    val difference: DifferenceModel?
) {

    class DifferenceModel(
        @DrawableRes val icon: Int,
        val text: CharSequence,
        @ColorRes val color: Int
    )
}

fun AmountChangeModel(
    from: CharSequence?,
    to: CharSequence,
    difference: CharSequence?,
    positive: Boolean?
): AmountChangeModel {
    val differenceModel = if (positive != null && difference != null) {
        val icon = if (positive) R.drawable.ic_double_chevron_up else R.drawable.ic_double_chevron_down

        DifferenceModel(
            icon = icon,
            text = difference,
            color = R.color.button_background_primary
        )
    } else {
        null
    }

    return AmountChangeModel(
        from = from,
        to = to,
        difference = differenceModel
    )
}
