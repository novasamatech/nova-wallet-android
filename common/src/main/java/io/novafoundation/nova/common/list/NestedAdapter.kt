package io.novafoundation.nova.common.list

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Orientation
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.inflateChild
import kotlinx.android.synthetic.main.item_nested_list.view.itemNestedList

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
            parent.inflateChild(R.layout.item_nested_list),
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
    view: View,
    private val nestedAdapter: ListAdapter<T, TH>,
    @Orientation orientation: Int,
    padding: Rect?,
    disableItemAnimations: Boolean,
) : ViewHolder(view) {

    init {
        view.itemNestedList.adapter = nestedAdapter
        view.itemNestedList.layoutManager = LinearLayoutManager(view.context, orientation, false)
        if (disableItemAnimations) view.itemNestedList.itemAnimator = null

        padding?.let {
            view.itemNestedList.setPadding(
                it.left.dp(view.context),
                it.top.dp(view.context),
                it.right.dp(view.context),
                it.bottom.dp(view.context)
            )
        }
    }

    fun bind(nestedList: List<T>) {
        nestedAdapter.submitList(nestedList)
    }
}
