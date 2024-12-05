package io.novafoundation.nova.feature_dapp_impl.presentation.favorites

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.ImageLoader
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.recyclerView.dragging.OnItemDragCallback
import io.novafoundation.nova.common.utils.recyclerView.dragging.StartDragListener
import io.novafoundation.nova.common.utils.recyclerView.dragging.prepareForDragging
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import io.novafoundation.nova.feature_external_sign_api.presentation.dapp.showDAppIcon
import java.util.Collections
import kotlinx.android.synthetic.main.item_dapp_favorite_dragable.view.itemDraggableFavoriteDAppIcon
import kotlinx.android.synthetic.main.item_dapp_favorite_dragable.view.itemDraggableFavoriteDAppSubtitle
import kotlinx.android.synthetic.main.item_dapp_favorite_dragable.view.itemDraggableFavoriteDAppTitle
import kotlinx.android.synthetic.main.item_dapp_favorite_dragable.view.itemDraggableFavoriteDappDragHandle
import kotlinx.android.synthetic.main.item_dapp_favorite_dragable.view.itemDraggableFavoriteDappFavoriteIcon

class DappDraggableFavoritesAdapter(
    private val imageLoader: ImageLoader,
    private val handler: Handler,
    private val startDragListener: StartDragListener
) : RecyclerView.Adapter<DappDraggableFavoritesViewHolder>(), OnItemDragCallback {

    interface Handler {
        fun onDAppClicked(dapp: DappModel)

        fun onDAppFavoriteClicked(dapp: DappModel)

        fun onItemOrderingChanged(dapps: List<DappModel>)
    }

    private val dapps = mutableListOf<DappModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DappDraggableFavoritesViewHolder {
        return DappDraggableFavoritesViewHolder(parent.inflateChild(R.layout.item_dapp_favorite_dragable), imageLoader, handler, startDragListener)
    }

    override fun getItemCount(): Int {
        return dapps.size
    }

    override fun onBindViewHolder(holder: DappDraggableFavoritesViewHolder, position: Int) {
        holder.bind(dapps[position])
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        Collections.swap(dapps, fromPosition, toPosition)
        notifyItemMoved(toPosition, fromPosition)
        handler.onItemOrderingChanged(dapps)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(dapps: List<DappModel>) {
        this.dapps.clear()
        this.dapps.addAll(dapps)
        notifyDataSetChanged()
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
        itemDraggableFavoriteDAppIcon.showDAppIcon(item.iconUrl, imageLoader)
        itemDraggableFavoriteDAppTitle.text = item.name
        itemDraggableFavoriteDAppSubtitle.text = item.description

        itemDraggableFavoriteDappDragHandle.prepareForDragging(this@DappDraggableFavoritesViewHolder, startDragListener)

        itemDraggableFavoriteDappFavoriteIcon.setOnClickListener { itemHandler.onDAppFavoriteClicked(item) }
        setOnClickListener { itemHandler.onDAppClicked(item) }
    }
}
