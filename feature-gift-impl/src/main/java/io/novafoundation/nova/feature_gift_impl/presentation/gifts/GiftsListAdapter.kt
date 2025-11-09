package io.novafoundation.nova.feature_gift_impl.presentation.gifts

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import coil.ImageLoader
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.AlphaColorFilter
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawableWithRipple
import io.novafoundation.nova.feature_account_api.presenatation.chain.setTokenIcon
import io.novafoundation.nova.feature_gift_impl.R
import io.novafoundation.nova.feature_gift_impl.databinding.ItemGiftBinding

class GiftsListAdapter(
    private val handler: Handler,
    private val imageLoader: ImageLoader
) : ListAdapter<GiftRVItem, GiftViewHolder>(GiftsDiffCallback) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): GiftViewHolder {
        return GiftViewHolder(
            ItemGiftBinding.inflate(parent.inflater(), parent, false),
            handler,
            imageLoader
        )
    }

    override fun onBindViewHolder(holder: GiftViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    interface Handler {

        fun onGiftClicked(referendum: GiftRVItem)
    }
}

private object GiftsDiffCallback : DiffUtil.ItemCallback<GiftRVItem>() {
    override fun areItemsTheSame(
        oldItem: GiftRVItem,
        newItem: GiftRVItem
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: GiftRVItem,
        newItem: GiftRVItem
    ): Boolean {
        return oldItem == newItem
    }
}

class GiftViewHolder(
    private val binder: ItemGiftBinding,
    private val handler: GiftsListAdapter.Handler,
    private val imageLoader: ImageLoader
) : BaseViewHolder(binder.root) {

    fun bind(item: GiftRVItem) = with(binder) {
        // Set content
        giftAmount.text = item.amount
        giftAssetIcon.setTokenIcon(item.assetIcon, imageLoader)
        giftCreationDate.text = item.subtitle
        giftImage.setImageResource(item.imageRes)

        root.setOnClickListener { handler.onGiftClicked(item) }

        val amountColor = if (item.isClaimed) R.color.text_secondary else R.color.text_primary
        val assetIconAlpha = if (item.isClaimed) 0.56f else 1f

        giftAmount.setTextColorRes(amountColor)
        giftAssetIcon.colorFilter = AlphaColorFilter(assetIconAlpha)

        giftAmountChevron.isVisible = !item.isClaimed
        root.background = if (item.isClaimed) background(R.color.block_background) else background(R.color.gift_block_background)
    }

    private fun background(@ColorRes colorRes: Int): Drawable {
        return context.getRoundedCornerDrawableWithRipple(fillColorRes = colorRes, cornerSizeInDp = 12)
    }
}
