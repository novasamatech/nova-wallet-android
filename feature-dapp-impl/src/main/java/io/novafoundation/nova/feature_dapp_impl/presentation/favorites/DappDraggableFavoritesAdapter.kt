package io.novafoundation.nova.feature_dapp_impl.presentation.favorites

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.ImageLoader
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.recyclerView.dragging.StartDragListener
import io.novafoundation.nova.common.utils.recyclerView.dragging.prepareForDragging
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModelDiffCallback
import io.novafoundation.nova.feature_external_sign_api.presentation.dapp.showDAppIcon
import kotlinx.android.synthetic.main.item_dapp_favorite_dragable.view.itemFavoriteDAppIcon
import kotlinx.android.synthetic.main.item_dapp_favorite_dragable.view.itemFavoriteDAppSubtitle
import kotlinx.android.synthetic.main.item_dapp_favorite_dragable.view.itemFavoriteDAppTitle
import kotlinx.android.synthetic.main.item_dapp_favorite_dragable.view.itemFavoriteDappDragHandle
import kotlinx.android.synthetic.main.item_dapp_favorite_dragable.view.itemFavoriteDappFavoriteIcon

class DappDraggableFavoritesAdapter(
    private val imageLoader: ImageLoader,
    private val handler: Handler,
    private val startDragListener: StartDragListener
) : ListAdapter<DappModel, DappDraggableFavoritesViewHolder>(DappModelDiffCallback) {

    interface Handler {
        fun onDAppClicked(dapp: DappModel)

        fun onDAppFavoriteClicked(dapp: DappModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DappDraggableFavoritesViewHolder {
        return DappDraggableFavoritesViewHolder(parent.inflateChild(R.layout.item_dapp_favorite_dragable), imageLoader, handler, startDragListener)
    }

    override fun onBindViewHolder(holder: DappDraggableFavoritesViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class DappDraggableFavoritesViewHolder(
    view: View,
    private val imageLoader: ImageLoader,
    private val itemHandler: DappDraggableFavoritesAdapter.Handler,
    private val startDragListener: StartDragListener
) : ViewHolder(view) {

    @SuppressLint("ClickableViewAccessibility")
    fun bind(item: DappModel) = with(itemView) {
        itemFavoriteDAppIcon.showDAppIcon(item.iconUrl, imageLoader)
        itemFavoriteDAppTitle.text = item.name
        itemFavoriteDAppSubtitle.text = item.description

        itemFavoriteDappDragHandle.prepareForDragging(this@DappDraggableFavoritesViewHolder, startDragListener)

        itemFavoriteDappFavoriteIcon.setOnClickListener { itemHandler.onDAppFavoriteClicked(item) }
        setOnClickListener { itemHandler.onDAppClicked(item) }
    }
}
