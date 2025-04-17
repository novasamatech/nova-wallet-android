package io.novafoundation.nova.feature_dapp_impl.presentation.tab

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import coil.ImageLoader
import coil.clear
import coil.load
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.ImageMonitor
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.loadOrHide
import io.novafoundation.nova.common.utils.setPathOrStopWatching
import io.novafoundation.nova.feature_dapp_impl.databinding.ItemBrowserTabBinding
import java.io.File

class BrowserTabsAdapter(
    private val imageLoader: ImageLoader,
    private val handler: Handler
) : ListAdapter<BrowserTabRvItem, BrowserTabViewHolder>(DiffCallback) {

    interface Handler {

        fun tabClicked(item: BrowserTabRvItem, view: View)

        fun tabCloseClicked(item: BrowserTabRvItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrowserTabViewHolder {
        return BrowserTabViewHolder(ItemBrowserTabBinding.inflate(parent.inflater(), parent, false), imageLoader, handler)
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
        return oldItem == newItem
    }
}

class BrowserTabViewHolder(
    private val binder: ItemBrowserTabBinding,
    private val imageLoader: ImageLoader,
    private val itemHandler: BrowserTabsAdapter.Handler,
) : BaseViewHolder(binder.root) {

    private val screenshotImageMonitor = ImageMonitor(
        imageView = binder.browserTabScreenshot,
        imageLoader = imageLoader
    )

    private val tabIconImageMonitor = ImageMonitor(
        imageView = binder.browserTabFavicon,
        imageLoader = imageLoader
    )

    fun bind(item: BrowserTabRvItem) = with(binder) {
        browserTabCard.setOnClickListener { itemHandler.tabClicked(item, browserTabScreenshot) }
        browserTabClose.setOnClickListener { itemHandler.tabCloseClicked(item) }
        browserTabSiteName.text = item.tabName

        browserTabScreenshot.load(item.tabScreenshotPath?.asFile(), imageLoader)
        screenshotImageMonitor.setPathOrStopWatching(item.tabScreenshotPath)

        if (item.knownDappIconUrl != null) {
            browserTabFavicon.load(item.knownDappIconUrl, imageLoader)
            tabIconImageMonitor.stopMonitoring()
        } else {
            browserTabFavicon.loadOrHide(item.tabFaviconPath?.asFile(), imageLoader)
            tabIconImageMonitor.setPathOrStopWatching(item.tabFaviconPath)
        }
    }

    override fun unbind() {
        screenshotImageMonitor.stopMonitoring()
        tabIconImageMonitor.stopMonitoring()

        with(binder) {
            browserTabScreenshot.clear()
            browserTabFavicon.clear()
        }
    }

    private fun String.asFile() = File(this)
}
