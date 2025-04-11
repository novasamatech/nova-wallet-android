package io.novafoundation.nova.feature_dapp_impl.presentation.main

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.ImageLoader
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_dapp_impl.databinding.ItemFavoriteDappBinding
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DAppClickHandler
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModelDiffCallback
import io.novafoundation.nova.feature_external_sign_api.presentation.dapp.showDAppIcon

class DappFavoritesAdapter(
    private val imageLoader: ImageLoader,
    private val handler: DAppClickHandler
) : ListAdapter<DappModel, FavoriteDappViewHolder>(DappModelDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteDappViewHolder {
        return FavoriteDappViewHolder(ItemFavoriteDappBinding.inflate(parent.inflater(), parent, false), imageLoader, handler)
    }

    override fun onBindViewHolder(holder: FavoriteDappViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class FavoriteDappViewHolder(
    private val binder: ItemFavoriteDappBinding,
    private val imageLoader: ImageLoader,
    private val itemHandler: DAppClickHandler,
) : ViewHolder(binder.root) {

    fun bind(item: DappModel) = with(binder) {
        itemFavoriteDAppIcon.showDAppIcon(item.iconUrl, imageLoader)
        itemFavoriteDAppTitle.text = item.name

        binder.root.setOnClickListener { itemHandler.onDAppClicked(item) }
    }
}
