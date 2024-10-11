package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.search

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_governance_impl.R

class DelegateSearchCountResultAdapter : RecyclerView.Adapter<DelegationSearchCountViewHolder>() {

    private var countString: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DelegationSearchCountViewHolder {
        val containerView = parent.inflateChild(R.layout.item_delegations_search_result_count)

        return DelegationSearchCountViewHolder(containerView)
    }

    override fun getItemCount(): Int {
        return 1
    }

    override fun onBindViewHolder(holder: DelegationSearchCountViewHolder, position: Int) {
        holder.bind(countString)
    }

    fun setSearchResultCount(countString: String?) {
        if (this.countString != countString) {
            this.countString = countString
            notifyItemChanged(0)
        }
    }
}

class DelegationSearchCountViewHolder(
    containerView: View
) : ViewHolder(containerView) {

    fun bind(countString: String?) {
        itemView.itemDelegationSearchCount.text = countString
    }
}
