package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view

import android.view.View
import com.facebook.shimmer.ShimmerFrameLayout
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible

fun <T> ExtendedLoadingState<T>.applyToView(contentView: View, shimmeringView: ShimmerFrameLayout, contentApplier: (T) -> Unit = {}) {
    when (this) {
        is ExtendedLoadingState.Error, ExtendedLoadingState.Loading -> {
            shimmeringView.makeVisible()
            shimmeringView.startShimmer()
            contentView.makeGone()
        }

        is ExtendedLoadingState.Loaded -> {
            shimmeringView.makeGone()
            shimmeringView.stopShimmer()
            contentView.makeVisible()

            contentApplier(data)
        }
    }
}
