package io.novafoundation.nova.feature_dapp_impl.presentation.main

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import coil.ImageLoader
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DAppClickHandler
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModelDiffCallback
import io.novafoundation.nova.feature_external_sign_api.presentation.dapp.showDAppIcon
import kotlinx.android.synthetic.main.item_favorite_dapp.view.itemFavoriteDAppIcon
import kotlinx.android.synthetic.main.item_favorite_dapp.view.itemFavoriteDAppTitle

class DappFavoritesAdapter(
    private val imageLoader: ImageLoader,
    private val handler: DAppClickHandler
) : ListAdapter<DappModel, FavoriteDappViewHolder>(DappModelDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteDappViewHolder {
        return FavoriteDappViewHolder(parent.inflateChild(R.layout.item_favorite_dapp), imageLoader, handler)
    }

    override fun onBindViewHolder(holder: FavoriteDappViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class FavoriteDappViewHolder(
    private val view: View,
    private val imageLoader: ImageLoader,
    private val itemHandler: DAppClickHandler,
) : ViewHolder(view) {

    fun bind(item: DappModel) = with(view) {
        itemFavoriteDAppIcon.showDAppIcon(item.iconUrl, imageLoader)
        itemFavoriteDAppTitle.text = item.name

        setOnClickListener { itemHandler.onDAppClicked(item) }
    }
}
