package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.list

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.list.model.DelegationsBannerModel
import kotlinx.android.synthetic.main.item_delegations_header.view.itemDelegationHeaderClose
import kotlinx.android.synthetic.main.item_delegations_header.view.itemDelegationHeaderDescriptionAction

class DelegateBannerAdapter(
    private val handler: Handler
) : ListAdapter<DelegationsBannerModel, DelegationsHeaderViewHolder>(DelegationsBannerDiffCallback()) {

    interface Handler {
        fun closeBanner()

        fun describeYourselfClicked()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DelegationsHeaderViewHolder {
        val containerView = parent.inflateChild(R.layout.item_delegations_header)

        return DelegationsHeaderViewHolder(containerView, handler)
    }

    override fun onBindViewHolder(holder: DelegationsHeaderViewHolder, position: Int) {}
}

private class DelegationsBannerDiffCallback : DiffUtil.ItemCallback<DelegationsBannerModel>() {

    override fun areItemsTheSame(oldItem: DelegationsBannerModel, newItem: DelegationsBannerModel): Boolean {
        return true
    }

    override fun areContentsTheSame(oldItem: DelegationsBannerModel, newItem: DelegationsBannerModel): Boolean {
        return true
    }
}

class DelegationsHeaderViewHolder(
    containerView: View,
    handler: DelegateBannerAdapter.Handler
) : ViewHolder(containerView) {

    init {
        with(containerView) {
            itemDelegationHeaderClose.setOnClickListener {
                handler.closeBanner()
            }
            itemDelegationHeaderDescriptionAction.setOnClickListener {
                handler.describeYourselfClicked()
            }
        }
    }
}
