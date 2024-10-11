package io.novafoundation.nova.common.list

import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import kotlinx.android.extensions.LayoutContainer

abstract class BaseListAdapter<T, VH : BaseViewHolder<*>>(diffCallback: DiffUtil.ItemCallback<T>) : ListAdapter<T, VH>(diffCallback) {

    override fun onViewRecycled(holder: VH) {
        holder.unbind()
    }
}

abstract class BaseViewHolder<B : ViewBinding>(val binder: B) : RecyclerView.ViewHolder(binder.root), LayoutContainer {

    override val containerView: View = binder.root

    open fun unbind() {}
}
