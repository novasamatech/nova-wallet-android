package io.novafoundation.nova.common.list

import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer

abstract class BaseListAdapter<T, VH : BaseViewHolder>(diffCallback: DiffUtil.ItemCallback<T>) : ListAdapter<T, VH>(diffCallback) {

    override fun onViewRecycled(holder: VH) {
        holder.unbind()
    }
}

abstract class BaseViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    abstract fun unbind()
}
