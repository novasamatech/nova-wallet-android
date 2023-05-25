package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.more.list

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.StubHolder
import io.novafoundation.nova.common.list.decoration.BackgroundDecoration

class StakingDAppsDecoration(context: Context) : BackgroundDecoration(
    context = context,
    outerHorizontalMarginDp = 16,
    innerVerticalPaddingDp = 4
) {

    override fun shouldApplyDecoration(holder: RecyclerView.ViewHolder): Boolean {
        return holder is StakingDappViewHolder || holder is StubHolder
    }
}
