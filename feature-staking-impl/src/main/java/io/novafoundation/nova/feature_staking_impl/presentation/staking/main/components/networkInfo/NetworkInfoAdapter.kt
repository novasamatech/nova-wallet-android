package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.feature_staking_impl.R

class NetworkInfoAdapter : BaseListAdapter<NetworkInfoItem, NetworkInfoHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetworkInfoHolder {
        val view = TableCellView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }

        return NetworkInfoHolder(view)
    }

    override fun onBindViewHolder(holder: NetworkInfoHolder, position: Int) {
        val isLast = position == currentList.size - 1

        holder.bind(getItem(position), isLast)
    }
}

private class DiffCallback : DiffUtil.ItemCallback<NetworkInfoItem>() {

    override fun areItemsTheSame(oldItem: NetworkInfoItem, newItem: NetworkInfoItem): Boolean {
        return oldItem.title == newItem.title
    }

    override fun areContentsTheSame(oldItem: NetworkInfoItem, newItem: NetworkInfoItem): Boolean {
        return oldItem.content == newItem.content
    }
}

class NetworkInfoHolder(override val containerView: TableCellView) : BaseViewHolder(containerView) {

    fun bind(item: NetworkInfoItem, isLast: Boolean) = with(containerView) {
        setTitle(item.title)

        when (val content = item.content) {
            is LoadingState.Loaded -> showValue(content.data.primary, content.data.secondary)
            is LoadingState.Loading -> showProgress()
        }

        setDividerColor(R.color.white_8)
        setDividerVisible(!isLast)
    }

    override fun unbind() {}
}
