package io.novafoundation.nova.feature_dapp_impl.presentation.main

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.decoration.BackgroundItemDecoration

class DAppItemDecoration(context: Context) : BackgroundItemDecoration(
    context = context,
    outerHorizontalMarginDp = 16,
    innerVerticalPaddingDp = 12
) {

    override fun shouldApplyDecoration(holder: RecyclerView.ViewHolder): Boolean {
        return holder !is HeaderHolder
    }
}
