package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.list

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_governance_impl.R

class DelegateSortAndFilterAdapter(
    private val handler: Handler
) : RecyclerView.Adapter<DelegationSortAndFilterHolder>() {

    interface Handler {
        fun filteringClicked()

        fun sortingClicked()
    }

    private var sortingValue: String? = null
    private var filteringValue: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DelegationSortAndFilterHolder {
        val containerView = parent.inflateChild(R.layout.item_delegations_sort_and_filter)

        return DelegationSortAndFilterHolder(containerView, handler)
    }

    override fun getItemCount(): Int {
        return 1
    }

    override fun onBindViewHolder(holder: DelegationSortAndFilterHolder, position: Int) {
        holder.bind(sortingValue, filteringValue)
    }

    fun setFilteringMixin(filtering: String) {
        filteringValue = filtering
        notifyItemChanged(0)
    }

    fun setSortingValue(sorting: String) {
        sortingValue = sorting
        notifyItemChanged(0)
    }
}

class DelegationSortAndFilterHolder(
    containerView: View,
    handler: DelegateSortAndFilterAdapter.Handler
) : ViewHolder(containerView) {

    init {
        with(containerView) {
            itemDelegateListSorting.setOnClickListener {
                handler.sortingClicked()
            }
            itemDelegateListFilters.setOnClickListener {
                handler.filteringClicked()
            }
        }
    }

    fun bind(sortingValue: String?, filteringValue: String?) {
        with(itemView) {
            itemDelegateListSorting.setValueDisplay(sortingValue)
            itemDelegateListFilters.setValueDisplay(filteringValue)
        }
    }
}
