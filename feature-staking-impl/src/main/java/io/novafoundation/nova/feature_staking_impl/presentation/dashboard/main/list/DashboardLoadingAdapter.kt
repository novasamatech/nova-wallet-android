package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.list

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_dashboard_loading.view.itemDashboardLoading

class DashboardLoadingAdapter : RecyclerView.Adapter<DashboardLoadingHolder>() {

    private var numberOfItems: Int = 0

    fun setNumberOfLoadingItems(loadingItems: Int) {
        val previousNumber = numberOfItems
        numberOfItems = loadingItems

        if (previousNumber < numberOfItems) {
            val itemsAdded = numberOfItems - previousNumber

            notifyItemRangeInserted(previousNumber, itemsAdded)
        } else if (previousNumber > numberOfItems) {
            val itemsRemoved = previousNumber - numberOfItems

            notifyItemRangeRemoved(numberOfItems, itemsRemoved)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardLoadingHolder {
        return DashboardLoadingHolder(parent.inflateChild(R.layout.item_dashboard_loading))
    }

    override fun onBindViewHolder(holder: DashboardLoadingHolder, position: Int) {
        holder.bind()
    }

    override fun onViewRecycled(holder: DashboardLoadingHolder) {
        holder.unbind()
    }

    override fun getItemCount(): Int {
        return numberOfItems
    }
}

class DashboardLoadingHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind() {
        containerView.itemDashboardLoading.startShimmer()
    }

    fun unbind() {
        containerView.itemDashboardLoading.stopShimmer()
    }
}
