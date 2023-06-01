package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view

import android.view.View
import com.facebook.shimmer.ShimmerFrameLayout
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setShimmerShown

class ShimmerableGroup<V : View>(val container: ShimmerFrameLayout, val shimmerShape: View? = null, val content: V)

data class SyncingData<T>(val data: T, val isSyncing: Boolean)

fun <T> T.syncingIf(isSyncing: Boolean) = SyncingData(this, isSyncing)

fun <V : View, T> ShimmerableGroup<V>.applyState(
    loadingState: ExtendedLoadingState<SyncingData<T>>,
    setContent: (V.(T) -> Unit)? = null
) {
    when (loadingState) {
        is ExtendedLoadingState.Error, ExtendedLoadingState.Loading -> {
            shimmerShape?.makeVisible()
            container.showShimmer(true)
            content.makeGone()
        }

        is ExtendedLoadingState.Loaded -> {
            shimmerShape?.makeGone()
            content.makeVisible()
            container.setShimmerShown(loadingState.data.isSyncing)

            setContent?.invoke(content, loadingState.data.data)
        }
    }
}
