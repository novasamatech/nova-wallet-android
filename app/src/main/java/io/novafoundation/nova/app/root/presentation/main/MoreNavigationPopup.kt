package io.novafoundation.nova.app.root.presentation.main

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.view.ViewCompat
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.databinding.PopupMoreNavigationBinding
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable

class MoreNavigationPopup(
    private val navigationBar: View,
    selectedDestinationId: Int?,
    onDestinationSelected: (Int) -> Unit
) : PopupWindow(navigationBar.context) {

    private val context = navigationBar.context

    private val binder = PopupMoreNavigationBinding.inflate(LayoutInflater.from(context))

    init {
        contentView = binder.root
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        isFocusable = true
        isOutsideTouchable = true
        inputMethodMode = INPUT_METHOD_NOT_NEEDED
        animationStyle = R.style.Animation_Nova_MoreNavigation
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binder.root.background = context.getRoundedCornerDrawable(
            fillColorRes = R.color.solid_navigation_background,
            strokeColorRes = R.color.container_border,
            cornerSizeInDp = 18
        )

        bindItem(binder.moreNavigationGovernance, R.id.voteFragment, R.drawable.ic_more_governance, selectedDestinationId, onDestinationSelected)
        bindItem(binder.moreNavigationSettings, R.id.profileFragment, R.drawable.ic_more_settings, selectedDestinationId, onDestinationSelected)
        ViewCompat.setAccessibilityPaneTitle(binder.root, context.getString(R.string.tabbar_more_title))
    }

    fun show() {
        val margin = 16.dp(context)
        width = minOf(238.dp(context), navigationBar.width - 2 * margin).coerceAtLeast(1)
        binder.root.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val barLocation = IntArray(2)
        navigationBar.getLocationInWindow(barLocation)
        val isRtl = navigationBar.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val x = if (isRtl) barLocation[0] + margin else barLocation[0] + navigationBar.width - width - margin
        val y = barLocation[1] - binder.root.measuredHeight - 8.dp(context)

        showAtLocation(navigationBar, Gravity.TOP or Gravity.LEFT, x, y)
    }

    private fun bindItem(
        view: TextView,
        destinationId: Int,
        icon: Int,
        selectedDestinationId: Int?,
        onDestinationSelected: (Int) -> Unit
    ) {
        val isSelected = destinationId == selectedDestinationId
        val iconColor = if (isSelected) R.color.icon_accent else R.color.icon_primary
        val textColor = if (isSelected) R.color.button_text_accent else R.color.text_primary
        view.isSelected = isSelected
        view.setTextColor(context.getColor(textColor))
        view.setDrawableEnd(icon, widthInDp = 22, heightInDp = 22, paddingInDp = 16, tint = iconColor)
        view.setOnClickListener {
            dismiss()
            onDestinationSelected(destinationId)
        }
    }
}
