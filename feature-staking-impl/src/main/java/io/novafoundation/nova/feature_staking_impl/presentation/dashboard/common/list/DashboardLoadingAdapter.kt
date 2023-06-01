package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.list

import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import io.novafoundation.nova.common.utils.inflateChild
import kotlinx.android.extensions.LayoutContainer

class DashboardLoadingAdapter(
    private val initialNumberOfItems: Int,
    @LayoutRes private val layout: Int,
) : RecyclerView.Adapter<DashboardLoadingHolder>() {

    private var numberOfItems: Int = initialNumberOfItems

    fun setLoaded(loaded: Boolean) {
        val newItems = if (loaded) 0 else initialNumberOfItems
        setNumberOfLoadingItems(newItems)
    }

    private fun setNumberOfLoadingItems(loadingItems: Int) {
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
        return DashboardLoadingHolder(parent.inflateChild(layout) as ViewGroup)
    }

    override fun onBindViewHolder(holder: DashboardLoadingHolder, position: Int) {
        holder.bind()
    }

    override fun onViewRecycled(holder: DashboardLoadingHolder) {
        holder.unbind()
    }

    override fun getItemViewType(position: Int): Int {
        return layout
    }

    override fun getItemCount(): Int {
        return numberOfItems
    }
}

class DashboardLoadingHolder(override val containerView: ViewGroup) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind() {
        containerView.children.forEach { (it as? ShimmerFrameLayout)?.startShimmer() }
    }

    fun unbind() {
        containerView.children.forEach { (it as? ShimmerFrameLayout)?.stopShimmer() }
    }
}
