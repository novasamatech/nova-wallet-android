package io.novafoundation.nova.feature_dapp_impl.presentation.browser.tabs

import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.load
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.feature_dapp_impl.R
import java.io.File

class BrowserTabsAdapter(
    private val imageLoader: ImageLoader,
    private val onTabClick: (TabStateModel) -> Unit
) : ListAdapter<TabStateModel, TabStateViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabStateViewHolder {
        return TabStateViewHolder(imageLoader, ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200.dp)
        })
    }

    override fun onBindViewHolder(holder: TabStateViewHolder, position: Int) {
        holder.bind(getItem(position), onTabClick)
    }
}

class TabStateViewHolder(
    private val imageLoader: ImageLoader,
    private val view: ImageView
) : RecyclerView.ViewHolder(view) {

    fun bind(item: TabStateModel, onTabClick: (TabStateModel) -> Unit) {
        view.load(item.imagePath?.let { File(it) }, imageLoader) {
            fallback(R.drawable.ic_browser_outline)
            error(R.drawable.ic_browser_outline)
        }
        view.setOnClickListener { onTabClick(item) }
    }
}

private object DiffCallback : DiffUtil.ItemCallback<TabStateModel>() {

    override fun areItemsTheSame(oldItem: TabStateModel, newItem: TabStateModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: TabStateModel, newItem: TabStateModel): Boolean {
        return false
    }
}
