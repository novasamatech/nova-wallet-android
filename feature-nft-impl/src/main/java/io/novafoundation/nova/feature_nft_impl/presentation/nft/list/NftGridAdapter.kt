package io.novafoundation.nova.feature_nft_impl.presentation.nft.list

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.clear
import coil.load
import coil.transform.RoundedCornersTransformation
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getRippleMask
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_nft_impl.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_nft_grid.view.itemNftContent
import kotlinx.android.synthetic.main.item_nft_grid.view.itemNftMedia
import kotlinx.android.synthetic.main.item_nft_grid.view.itemNftShimmer
import kotlinx.android.synthetic.main.item_nft_grid.view.itemNftTitle
import kotlinx.android.synthetic.main.item_nft_grid.view.nftCollectionName

class NftGridAdapter(
    private val imageLoader: ImageLoader,
    private val handler: Handler
) : ListAdapter<NftListItem, NftHolder>(DiffCallback) {

    interface Handler {

        fun itemClicked(item: NftListItem)

        fun loadableItemShown(item: NftListItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NftHolder {
        return NftHolder(parent.inflateChild(R.layout.item_nft_grid), imageLoader, handler)
    }

    override fun onBindViewHolder(holder: NftHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: NftHolder) {
        holder.unbind()
    }
}

private object DiffCallback : DiffUtil.ItemCallback<NftListItem>() {

    override fun areItemsTheSame(oldItem: NftListItem, newItem: NftListItem): Boolean {
        return oldItem.identifier == newItem.identifier
    }

    override fun areContentsTheSame(oldItem: NftListItem, newItem: NftListItem): Boolean {
        return oldItem == newItem
    }
}

class NftHolder(
    override val containerView: View,
    private val imageLoader: ImageLoader,
    private val itemHandler: NftGridAdapter.Handler
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        with(containerView) {
            itemNftContent.background = with(context) {
                addRipple(getRoundedCornerDrawable(R.color.block_background, cornerSizeInDp = 12), mask = getRippleMask(cornerSizeDp = 12))
            }
        }
    }

    fun unbind() = with(containerView) {
        itemNftMedia.clear()
    }

    fun bind(item: NftListItem) = with(containerView) {
        when (val content = item.content) {
            is LoadingState.Loading -> {
                itemNftShimmer.makeVisible()
                itemNftShimmer.startShimmer()
                itemNftContent.makeGone()

                itemHandler.loadableItemShown(item)
            }
            is LoadingState.Loaded -> {
                itemNftShimmer.makeGone()
                itemNftShimmer.stopShimmer()
                itemNftContent.makeVisible()

                itemNftMedia.load(content.data.media, imageLoader) {
                    transformations(RoundedCornersTransformation(8.dpF(context)))
                    placeholder(R.drawable.nft_media_progress)
                    error(R.drawable.nft_media_error)
                    fallback(R.drawable.nft_media_error)
                    listener(
                        onError = { _, _ ->
                            // so that placeholder would be able to change aspect ratio and fill ImageView entirely
                            itemNftMedia.scaleType = ImageView.ScaleType.FIT_XY
                        },
                        onSuccess = { _, _ ->
                            // set default scale type back
                            itemNftMedia.scaleType = ImageView.ScaleType.FIT_CENTER
                        }
                    )
                }

                nftCollectionName.text = content.data.collectionName
                itemNftTitle.text = content.data.title
            }
        }

        setOnClickListener { itemHandler.itemClicked(item) }
    }
}
