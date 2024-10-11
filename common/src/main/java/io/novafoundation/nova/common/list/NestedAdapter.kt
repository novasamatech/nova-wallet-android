package io.novafoundation.nova.common.list

import android.graphics.Rect
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Orientation
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ItemNestedListBinding
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.inflateChild

class NestedAdapter<T, TH : ViewHolder>(
    private val nestedAdapter: ListAdapter<T, TH>,
    @Orientation private val orientation: Int,
    private val paddingInDp: Rect? = null,
    private val disableItemAnimations: Boolean = false,
) : RecyclerView.Adapter<NestedListViewHolder<T, TH>>() {

    private var showNestedList = true
    private var nestedList: List<T> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NestedListViewHolder<T, TH> {
        return NestedListViewHolder(
            ItemNestedListBinding.bind(parent),
            nestedAdapter,
            orientation,
            paddingInDp,
            disableItemAnimations
        )
    }

    override fun onBindViewHolder(holder: NestedListViewHolder<T, TH>, position: Int) {
        holder.bind(nestedList)
    }

    override fun getItemCount(): Int {
        return if (showNestedList) 1 else 0
    }

    fun submitList(items: List<T>) {
        nestedList = items
        if (showNestedList) {
            notifyItemChanged(0, true)
        }
    }

    fun show(show: Boolean) {
        if (showNestedList != show) {
            showNestedList = show
            if (show) {
                notifyItemInserted(0)
            } else {
                notifyItemRemoved(0)
            }
        }
    }
}

class NestedListViewHolder<T, TH : ViewHolder>(
    binder: ItemNestedListBinding,
    private val nestedAdapter: ListAdapter<T, TH>,
    @Orientation orientation: Int,
    padding: Rect?,
    disableItemAnimations: Boolean,
) : BaseViewHolder<ItemNestedListBinding>(binder) {

    init {
        binder.itemNestedList.adapter = nestedAdapter
        binder.itemNestedList.layoutManager = LinearLayoutManager(binder.root.context, orientation, false)
        if (disableItemAnimations) binder.itemNestedList.itemAnimator = null

        padding?.let {
            binder.itemNestedList.setPadding(
                it.left.dp(binder.root.context),
                it.top.dp(binder.root.context),
                it.right.dp(binder.root.context),
                it.bottom.dp(binder.root.context)
            )
        }
    }

    fun bind(nestedList: List<T>) {
        nestedAdapter.submitList(nestedList)
    }
}
