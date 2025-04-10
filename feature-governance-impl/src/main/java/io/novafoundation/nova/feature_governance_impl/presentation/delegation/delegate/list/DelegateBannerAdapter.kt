package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_governance_impl.databinding.ItemDelegationsHeaderBinding

class DelegateBannerAdapter(
    private val handler: Handler
) : RecyclerView.Adapter<DelegationsHeaderViewHolder>() {

    interface Handler {
        fun closeBanner()

        fun describeYourselfClicked()
    }

    private var showBanner: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DelegationsHeaderViewHolder {
        return DelegationsHeaderViewHolder(ItemDelegationsHeaderBinding.inflate(parent.inflater(), parent, false), handler)
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
    binder: ItemDelegationsHeaderBinding,
    handler: DelegateBannerAdapter.Handler
) : ViewHolder(binder.root) {

    init {
        with(binder) {
            itemDelegationBanner.setOnCloseClickListener {
                handler.closeBanner()
            }
            delegateBannerMoreContent.setOnClickListener {
                handler.describeYourselfClicked()
            }
        }
    }
}
