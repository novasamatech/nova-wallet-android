package io.novafoundation.nova.feature_pay_impl.presentation.shop

import android.view.View
import android.view.ViewGroup
import com.facebook.shimmer.ShimmerFrameLayout
import io.novafoundation.nova.common.list.CustomPlaceholderAdapter
import io.novafoundation.nova.common.list.StubHolder
import io.novafoundation.nova.common.utils.setBackgroundTintRes
import io.novafoundation.nova.common.utils.traverseViews
import io.novafoundation.nova.feature_pay_impl.R

class ShopUnavailableAccountPlaceholderAdapter : CustomPlaceholderAdapter(R.layout.fragment_shop_placeholder) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StubHolder {
        val holder = super.onCreateViewHolder(parent, viewType)
        disableShimmeringFor(holder.itemView)
        return holder
    }

    private fun disableShimmeringFor(view: View) {
        view.traverseViews {
            if (it is ShimmerFrameLayout) {
                it.hideShimmer()
                it.setBackgroundTintRes(R.color.shimmering_indicator_background)
            }
        }
    }
}
