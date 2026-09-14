package io.novafoundation.nova.feature_assets.presentation.tokens.manage

import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import coil.clear
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.recyclerView.expandable.ExpandableAdapter
import io.novafoundation.nova.common.utils.recyclerView.expandable.ExpandableBaseViewHolder
import io.novafoundation.nova.common.utils.recyclerView.expandable.ExpandableChildViewHolder
import io.novafoundation.nova.common.utils.recyclerView.expandable.ExpandableParentViewHolder
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableBaseItem
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableChildItem
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableParentItem
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.ItemManageAssetChildNetworkBinding
import io.novafoundation.nova.feature_assets.databinding.ItemManageAssetChildTokenBinding
import io.novafoundation.nova.feature_assets.databinding.ItemManageAssetGroupBinding
import io.novafoundation.nova.feature_assets.databinding.ItemManageAssetHeaderBinding
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.model.ManageAssetIconModel
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.model.ManageAssetsRvItem

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_GROUP = 1

// A group of tokens expands into networks, a group of networks expands into tokens, and the design
// gives those two children different rows - so the icon a child carries picks its layout.
private const val VIEW_TYPE_CHILD_NETWORK = 2
private const val VIEW_TYPE_CHILD_TOKEN = 3

private const val CHEVRON_EXPANDED_ROTATION = 180f
private const val CHEVRON_ROTATION_DURATION = 400L

private const val DISABLED_ICON_ALPHA = 0.48f

class ManageTokensAdapter(
    private val imageLoader: ImageLoader,
    private val handler: ItemHandler
) : BaseListAdapter<ManageAssetsRvItem, BaseViewHolder>(DiffCallback()), ExpandableAdapter {

    interface ItemHandler {

        fun groupClicked(position: Int)

        fun groupSwitched(position: Int)

        fun childSwitched(position: Int)
    }

    override fun getItems(): List<ExpandableBaseItem> = currentList

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is ManageAssetsRvItem.Header -> VIEW_TYPE_HEADER
            is ManageAssetsRvItem.Group -> VIEW_TYPE_GROUP
            is ManageAssetsRvItem.Child -> when (item.icon) {
                is ManageAssetIconModel.Network -> VIEW_TYPE_CHILD_NETWORK
                is ManageAssetIconModel.Token -> VIEW_TYPE_CHILD_TOKEN
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = parent.inflater()

        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(ItemManageAssetHeaderBinding.inflate(inflater, parent, false))
            VIEW_TYPE_GROUP -> GroupViewHolder(ItemManageAssetGroupBinding.inflate(inflater, parent, false), handler, imageLoader)
            VIEW_TYPE_CHILD_TOKEN -> TokenChildViewHolder(ItemManageAssetChildTokenBinding.inflate(inflater, parent, false), handler, imageLoader)
            else -> NetworkChildViewHolder(ItemManageAssetChildNetworkBinding.inflate(inflater, parent, false), handler, imageLoader)
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int, payloads: MutableList<Any>) {
        val item = getItem(position)

        if (item !is ManageAssetsRvItem.Group || holder !is GroupViewHolder) {
            super.onBindViewHolder(holder, position, payloads)
            return
        }

        resolvePayload(holder, position, payloads) {
            when (it) {
                // Only the chevron reacts: rebinding the whole row here would cancel the rotation
                ManageAssetsRvItem.Group::expanded -> holder.bindExpanded(item, animate = true)
            }
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ManageAssetsRvItem.Header -> (holder as HeaderViewHolder).bind(item)
            is ManageAssetsRvItem.Group -> (holder as GroupViewHolder).bind(item)
            is ManageAssetsRvItem.Child -> when (holder) {
                is TokenChildViewHolder -> holder.bind(item)
                is NetworkChildViewHolder -> holder.bind(item)
                else -> Unit
            }
        }
    }
}

private fun ImageView.setManageAssetIcon(icon: ManageAssetIconModel, imageLoader: ImageLoader) {
    when (icon) {
        is ManageAssetIconModel.Token -> setIcon(icon.icon, imageLoader)
        is ManageAssetIconModel.Network -> loadChainIcon(icon.url, imageLoader)
    }
}

private fun ImageView.applyEnabledLook(enabled: Boolean) {
    alpha = if (enabled) 1f else DISABLED_ICON_ALPHA
}

private fun android.widget.TextView.applyEnabledLook(enabled: Boolean) {
    setTextColorRes(if (enabled) R.color.text_primary else R.color.text_secondary)
}

private class HeaderViewHolder(
    private val binder: ItemManageAssetHeaderBinding
) : BaseViewHolder(binder.root), ExpandableBaseViewHolder<ExpandableBaseItem> {

    override var expandableItem: ExpandableBaseItem? = null

    fun bind(item: ManageAssetsRvItem.Header) {
        // Only animateMove looks at this, so a section title slides instead of jumping
        updateExpandableItem(item)

        binder.itemManageAssetHeaderTitle.text = item.title
    }

    override fun unbind() {
        // Nothing to release
    }
}

private class GroupViewHolder(
    private val binder: ItemManageAssetGroupBinding,
    itemHandler: ManageTokensAdapter.ItemHandler,
    private val imageLoader: ImageLoader
) : BaseViewHolder(binder.root), ExpandableParentViewHolder {

    override var expandableItem: ExpandableParentItem? = null

    init {
        binder.root.setOnClickListener { itemHandler.groupClicked(bindingAdapterPosition) }
        binder.itemManageAssetGroupEnabled.setOnClickListener { itemHandler.groupSwitched(bindingAdapterPosition) }
    }

    fun bind(item: ManageAssetsRvItem.Group) = with(binder) {
        // The animator reads this to tell which group a moving row belongs to
        updateExpandableItem(item)

        itemManageAssetGroupIcon.setManageAssetIcon(item.icon, imageLoader)
        itemManageAssetGroupTitle.text = item.title
        itemManageAssetGroupSubtitle.text = item.subtitle

        itemManageAssetGroupEnabled.isChecked = item.enabled
        itemManageAssetGroupEnabled.isEnabled = item.switchable

        itemManageAssetGroupIcon.applyEnabledLook(item.enabled)
        itemManageAssetGroupTitle.applyEnabledLook(item.enabled)

        itemManageAssetGroupExpand.setVisible(item.expandable)

        bindExpanded(item, animate = false)
    }

    fun bindExpanded(item: ManageAssetsRvItem.Group, animate: Boolean) {
        val targetRotation = if (item.expanded) CHEVRON_EXPANDED_ROTATION else 0f

        with(binder.itemManageAssetGroupExpand) {
            animate().cancel()

            if (animate) {
                animate().rotation(targetRotation).setDuration(CHEVRON_ROTATION_DURATION).start()
            } else {
                rotation = targetRotation
            }
        }
    }

    override fun unbind() {
        binder.itemManageAssetGroupIcon.clear()
    }
}

private class NetworkChildViewHolder(
    private val binder: ItemManageAssetChildNetworkBinding,
    itemHandler: ManageTokensAdapter.ItemHandler,
    private val imageLoader: ImageLoader
) : BaseViewHolder(binder.root), ExpandableChildViewHolder {

    override var expandableItem: ExpandableChildItem? = null

    init {
        binder.itemManageAssetChildNetworkEnabled.setOnClickListener { itemHandler.childSwitched(bindingAdapterPosition) }
    }

    fun bind(item: ManageAssetsRvItem.Child) = with(binder) {
        // Without this the animator skips add and remove entirely - it is how a row finds its group
        updateExpandableItem(item)

        itemManageAssetChildNetworkIcon.setManageAssetIcon(item.icon, imageLoader)
        itemManageAssetChildNetworkTitle.text = item.title

        itemManageAssetChildNetworkEnabled.isChecked = item.enabled
        itemManageAssetChildNetworkEnabled.isEnabled = item.switchable

        itemManageAssetChildNetworkIcon.applyEnabledLook(item.enabled)
        itemManageAssetChildNetworkTitle.applyEnabledLook(item.enabled)
    }

    override fun unbind() {
        binder.itemManageAssetChildNetworkIcon.clear()
    }
}

private class TokenChildViewHolder(
    private val binder: ItemManageAssetChildTokenBinding,
    itemHandler: ManageTokensAdapter.ItemHandler,
    private val imageLoader: ImageLoader
) : BaseViewHolder(binder.root), ExpandableChildViewHolder {

    override var expandableItem: ExpandableChildItem? = null

    init {
        binder.itemManageAssetChildTokenEnabled.setOnClickListener { itemHandler.childSwitched(bindingAdapterPosition) }
    }

    fun bind(item: ManageAssetsRvItem.Child) = with(binder) {
        updateExpandableItem(item)

        itemManageAssetChildTokenIcon.setManageAssetIcon(item.icon, imageLoader)
        itemManageAssetChildTokenTitle.text = item.title
        itemManageAssetChildTokenSubtitle.text = item.subtitle
        itemManageAssetChildTokenSubtitle.setVisible(item.subtitle != null)

        itemManageAssetChildTokenEnabled.isChecked = item.enabled
        itemManageAssetChildTokenEnabled.isEnabled = item.switchable

        itemManageAssetChildTokenIcon.applyEnabledLook(item.enabled)
        itemManageAssetChildTokenTitle.applyEnabledLook(item.enabled)
    }

    override fun unbind() {
        binder.itemManageAssetChildTokenIcon.clear()
    }
}

private class DiffCallback : DiffUtil.ItemCallback<ManageAssetsRvItem>() {

    private val groupPayloadGenerator = PayloadGenerator(ManageAssetsRvItem.Group::expanded)

    override fun areItemsTheSame(oldItem: ManageAssetsRvItem, newItem: ManageAssetsRvItem): Boolean {
        return oldItem::class == newItem::class && oldItem.key == newItem.key
    }

    override fun areContentsTheSame(oldItem: ManageAssetsRvItem, newItem: ManageAssetsRvItem): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: ManageAssetsRvItem, newItem: ManageAssetsRvItem): Any? {
        if (oldItem !is ManageAssetsRvItem.Group || newItem !is ManageAssetsRvItem.Group) return null

        return groupPayloadGenerator.diff(oldItem, newItem)
    }
}
