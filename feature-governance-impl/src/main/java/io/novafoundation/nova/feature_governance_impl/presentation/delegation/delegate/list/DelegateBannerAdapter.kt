package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.list

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_governance_impl.R

class DelegateBannerAdapter(
    private val handler: Handler
) : RecyclerView.Adapter<DelegationsHeaderViewHolder>() {

    interface Handler {
        fun closeBanner()

        fun describeYourselfClicked()
    }

    private var showBanner: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DelegationsHeaderViewHolder {
        val containerView = parent.inflateChild(R.layout.item_delegations_header)

        return DelegationsHeaderViewHolder(containerView, handler)
    }

    override fun getItemCount(): Int {
        return if (showBanner) 1 else 0
    }

    override fun onBindViewHolder(holder: DelegationsHeaderViewHolder, position: Int) {}

    fun showBanner(show: Boolean) {
        if (showBanner != show) {
            showBanner = show
            if (show) {
                notifyItemInserted(0)
            } else {
                notifyItemRemoved(0)
            }
        }
    }
}

class DelegationsHeaderViewHolder(
    containerView: View,
    handler: DelegateBannerAdapter.Handler
) : ViewHolder(containerView) {

    init {
        with(containerView) {
            itemDelegationBanner.setOnCloseClickListener {
                handler.closeBanner()
            }
            delegateBannerMoreContent.setOnClickListener {
                handler.describeYourselfClicked()
            }
        }
    }
}
