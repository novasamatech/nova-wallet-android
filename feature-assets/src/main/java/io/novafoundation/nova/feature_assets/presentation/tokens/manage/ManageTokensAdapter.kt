package io.novafoundation.nova.feature_assets.presentation.tokens.manage

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import coil.ImageLoader
import coil.clear
import io.novafoundation.nova.common.list.BaseListAdapter
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.ItemManageTokenChildBinding
import io.novafoundation.nova.feature_assets.databinding.ItemManageTokenHeaderBinding
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.model.ManageTokensRvItem

private const val TYPE_HEADER = 0
private const val TYPE_CHILD = 1

class ManageTokensAdapter(
    private val imageLoader: ImageLoader,
    private val handler: ItemHandler
) : BaseListAdapter<ManageTokensRvItem, BaseViewHolder>(DiffCallback()) {

    interface ItemHandler {

        fun headerClicked(headerId: String)

        fun childToggled(item: ManageTokensRvItem.Child)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ManageTokensRvItem.Header -> TYPE_HEADER
            is ManageTokensRvItem.Child -> TYPE_CHILD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                binder = ItemManageTokenHeaderBinding.inflate(parent.inflater(), parent, false),
                itemHandler = handler,
                imageLoader = imageLoader
            )
            TYPE_CHILD -> ChildViewHolder(
                binder = ItemManageTokenChildBinding.inflate(parent.inflater(), parent, false),
                itemHandler = handler,
                imageLoader = imageLoader
            )
            else -> throw IllegalStateException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ManageTokensRvItem.Header -> (holder as HeaderViewHolder).bind(item)
            is ManageTokensRvItem.Child -> (holder as ChildViewHolder).bind(item)
        }
    }
}

class HeaderViewHolder(
    private val binder: ItemManageTokenHeaderBinding,
    private val itemHandler: ManageTokensAdapter.ItemHandler,
    private val imageLoader: ImageLoader,
) : BaseViewHolder(binder.root) {

    private var currentId: String? = null

    init {
        binder.root.setOnClickListener {
            currentId?.let { itemHandler.headerClicked(it) }
        }
    }

    fun bind(item: ManageTokensRvItem.Header) = with(binder) {
        currentId = item.id

        manageTokenHeaderTitle.text = item.title
        manageTokenHeaderCount.text = root.context.getString(
            R.string.manage_tokens_enabled_count,
            item.enabledCount,
            item.totalCount
        )

        item.icon?.let { manageTokenHeaderIcon.setIcon(it, imageLoader) }
        manageTokenHeaderIcon.setVisible(item.icon != null)

        manageTokenHeaderChevron.setImageResource(R.drawable.ic_chevron_down)
        manageTokenHeaderChevron.rotation = if (item.isExpanded) 180f else 0f

        val alpha = if (item.enabledCount > 0) 1f else 0.48f
        manageTokenHeaderIcon.alpha = alpha

        val contentColorRes = if (item.enabledCount > 0) R.color.text_primary else R.color.text_secondary
        manageTokenHeaderTitle.setTextColorRes(contentColorRes)
    }

    override fun unbind() {
        binder.manageTokenHeaderIcon.clear()
    }
}

class ChildViewHolder(
    private val binder: ItemManageTokenChildBinding,
    private val itemHandler: ManageTokensAdapter.ItemHandler,
    private val imageLoader: ImageLoader,
) : BaseViewHolder(binder.root) {

    private var currentItem: ManageTokensRvItem.Child? = null

    init {
        binder.manageTokenChildCheckbox.setOnClickListener {
            currentItem?.let { itemHandler.childToggled(it) }
        }
    }

    fun bind(item: ManageTokensRvItem.Child) = with(binder) {
        currentItem = item

        manageTokenChildName.text = item.name
        item.icon?.let { manageTokenChildIcon.setIcon(it, imageLoader) }
        manageTokenChildIcon.setVisible(item.icon != null)

        manageTokenChildCheckbox.isChecked = item.isEnabled
        manageTokenChildCheckbox.isEnabled = item.isSwitchable

        val contentColorRes = if (item.isEnabled) R.color.text_primary else R.color.text_secondary
        manageTokenChildName.setTextColorRes(contentColorRes)
    }

    override fun unbind() {
        binder.manageTokenChildIcon.clear()
    }
}

private class DiffCallback : DiffUtil.ItemCallback<ManageTokensRvItem>() {

    override fun areItemsTheSame(oldItem: ManageTokensRvItem, newItem: ManageTokensRvItem): Boolean {
        return when {
            oldItem is ManageTokensRvItem.Header && newItem is ManageTokensRvItem.Header -> oldItem.id == newItem.id
            oldItem is ManageTokensRvItem.Child && newItem is ManageTokensRvItem.Child -> oldItem.chainAssetId == newItem.chainAssetId
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: ManageTokensRvItem, newItem: ManageTokensRvItem): Boolean {
        return oldItem == newItem
    }
}
