package io.novafoundation.nova.common.list

import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflateChild

class StubItemsAdapter(
    private val itemCount: Int,
    @LayoutRes private val itemRes: Int,
    showByDefault: Boolean = false
) : RecyclerView.Adapter<RaisePayBrandsShimmerHolder>() {

    private var showStubs: Boolean = showByDefault

    fun show(showLoading: Boolean) {
        this.showStubs = showLoading

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RaisePayBrandsShimmerHolder {
        return RaisePayBrandsShimmerHolder(parent, itemRes)
    }

    override fun getItemCount(): Int {
        return if (showStubs) itemCount else 0
    }

    override fun onBindViewHolder(holder: RaisePayBrandsShimmerHolder, position: Int) {}
}

class RaisePayBrandsShimmerHolder(
    parent: ViewGroup,
    itemRes: Int,
) : RecyclerView.ViewHolder(parent.inflateChild(itemRes))
