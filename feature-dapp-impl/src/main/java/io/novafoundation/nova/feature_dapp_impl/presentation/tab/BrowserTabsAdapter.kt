package io.novafoundation.nova.feature_dapp_impl.presentation.tab

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import coil.ImageLoader
import coil.clear
import coil.load
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.coil.transformation.TopCropTransformation
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.loadOrHide
import io.novafoundation.nova.feature_dapp_impl.R
import java.io.File
import kotlinx.android.synthetic.main.item_browser_tab.view.browserTabCard
import kotlinx.android.synthetic.main.item_browser_tab.view.browserTabClose
import kotlinx.android.synthetic.main.item_browser_tab.view.browserTabFavicon
import kotlinx.android.synthetic.main.item_browser_tab.view.browserTabScreenshot
import kotlinx.android.synthetic.main.item_browser_tab.view.browserTabSiteName

class BrowserTabsAdapter(
    private val imageLoader: ImageLoader,
    private val handler: Handler
) : ListAdapter<BrowserTabRvItem, BrowserTabViewHolder>(DiffCallback) {

    interface Handler {

        fun tabClicked(item: BrowserTabRvItem)

        fun tabCloseClicked(item: BrowserTabRvItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrowserTabViewHolder {
        return BrowserTabViewHolder(parent.inflateChild(R.layout.item_browser_tab), imageLoader, handler)
    }

    override fun onBindViewHolder(holder: BrowserTabViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private object DiffCallback : DiffUtil.ItemCallback<BrowserTabRvItem>() {

    override fun areItemsTheSame(oldItem: BrowserTabRvItem, newItem: BrowserTabRvItem): Boolean {
        return oldItem.tabId == newItem.tabId
    }

    override fun areContentsTheSame(oldItem: BrowserTabRvItem, newItem: BrowserTabRvItem): Boolean {
        return true
    }
}

class BrowserTabViewHolder(
    private val view: View,
    private val imageLoader: ImageLoader,
    private val itemHandler: BrowserTabsAdapter.Handler,
) : BaseViewHolder(view) {

    fun bind(item: BrowserTabRvItem) = with(itemView) {
        browserTabCard.setOnClickListener { itemHandler.tabClicked(item) }
        browserTabClose.setOnClickListener { itemHandler.tabCloseClicked(item) }
        browserTabScreenshot.load(item.tabScreenshotPath?.asFile(), imageLoader) {
            transformations(TopCropTransformation())
        }
        browserTabFavicon.loadOrHide(item.tabFaviconPath?.asFile(), imageLoader)
        browserTabSiteName.text = item.tabName
    }

    override fun unbind() {
        with(itemView) {
            browserTabScreenshot.clear()
            browserTabFavicon.clear()
        }
    }

    private fun String.asFile() = File(this)
}
