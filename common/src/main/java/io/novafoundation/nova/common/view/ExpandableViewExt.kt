package io.novafoundation.nova.common.view

import android.widget.TextView
import io.novafoundation.nova.common.R

fun ExpandableView.bindWithHideShowButton(hideShowButton: TextView) {
    hideShowButton.setHideShowButtonState(currentState())
    setCallback { hideShowButton.setHideShowButtonState(it) }
}

private fun TextView.setHideShowButtonState(state: ExpandableViewState) {
    when (state) {
        ExpandableViewState.COLLAPSED -> {
            text = context.getString(R.string.common_show)
        }

        ExpandableViewState.EXPANDED -> {
            text = context.getString(R.string.common_hide)
        }
    }
}
