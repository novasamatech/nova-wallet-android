package io.novafoundation.nova.feature_governance_impl.presentation.common.locks

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.themed
import io.novafoundation.nova.feature_governance_impl.R

class AmountChipModel(
    val amountInput: String,
    val label: String
)

fun LinearLayout.setChips(
    newChips: List<AmountChipModel>,
    onClicked: (AmountChipModel) -> Unit,
    scrollingParent: View
) {
    scrollingParent.setVisible(newChips.isNotEmpty())
    removeAllViews()

    newChips.forEach { chipModel ->
        val view = ChipView(context).apply {
            text = chipModel.label

            setOnClickListener { onClicked(chipModel) }
        }

        addView(view)
    }
}

private fun ChipView(context: Context) = TextView(context.themed(R.style.Widget_Nova_Action_Secondary)).apply {
    layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        marginEnd = 8.dp(context)
    }
}
