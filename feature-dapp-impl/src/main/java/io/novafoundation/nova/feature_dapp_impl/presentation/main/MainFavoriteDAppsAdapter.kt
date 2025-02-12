package io.novafoundation.nova.feature_dapp_impl.presentation.main

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import io.novafoundation.nova.common.list.SingleItemAdapter
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DAppClickHandler
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import kotlinx.android.synthetic.main.item_main_favorite_dapps.view.dAppMainFavoriteDAppList
import kotlinx.android.synthetic.main.item_main_favorite_dapps.view.dAppMainFavoriteDAppTitle
import kotlinx.android.synthetic.main.item_main_favorite_dapps.view.dAppMainFavoriteDAppsShow

class MainFavoriteDAppsAdapter(
    private val dappClickHandler: DAppClickHandler,
    private val handler: Handler,
    private val imageLoader: ImageLoader
) : SingleItemAdapter<FavoriteDAppHolder>(isShownByDefault = true) {

    interface Handler {
        fun onManageFavoritesClick()
    }

    private var favoritesDApps: List<DappModel> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteDAppHolder {
        return FavoriteDAppHolder(parent.inflateChild(R.layout.item_main_favorite_dapps), imageLoader, dappClickHandler, handler)
    }

    override fun onBindViewHolder(holder: FavoriteDAppHolder, position: Int) {
        holder.bind(favoritesDApps)
    }

    fun setDApps(dapps: List<DappModel>) {
        favoritesDApps = dapps
        notifyChangedIfShown()
    }
}

class FavoriteDAppHolder(
    view: View,
    imageLoader: ImageLoader,
    dAppClickHandler: DAppClickHandler,
    handler: MainFavoriteDAppsAdapter.Handler
) : RecyclerView.ViewHolder(view) {

    private val favoritesAdapter = DappFavoritesAdapter(imageLoader, dAppClickHandler)

    init {
        itemView.dAppMainFavoriteDAppList.adapter = favoritesAdapter
        itemView.dAppMainFavoriteDAppsShow.setOnClickListener { handler.onManageFavoritesClick() }
    }

    fun bind(dapps: List<DappModel>) = with(itemView) {
        dAppMainFavoriteDAppList.isGone = dapps.isEmpty()
        dAppMainFavoriteDAppTitle.isGone = dapps.isEmpty()
        dAppMainFavoriteDAppsShow.isGone = dapps.isEmpty()

        favoritesAdapter.submitList(dapps)
    }
}
