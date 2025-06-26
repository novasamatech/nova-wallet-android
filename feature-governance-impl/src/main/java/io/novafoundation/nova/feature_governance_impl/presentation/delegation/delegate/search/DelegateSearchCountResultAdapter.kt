package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.search

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_governance_impl.databinding.ItemDelegationsSearchResultCountBinding

class DelegateSearchCountResultAdapter : RecyclerView.Adapter<DelegationSearchCountViewHolder>() {

    private var countString: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DelegationSearchCountViewHolder {
        return DelegationSearchCountViewHolder(ItemDelegationsSearchResultCountBinding.inflate(parent.inflater(), parent, false))
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
    private val binder: ItemDelegationsSearchResultCountBinding
) : ViewHolder(binder.root) {

    fun bind(countString: String?) {
        binder.itemDelegationSearchCount.text = countString
    }
}
