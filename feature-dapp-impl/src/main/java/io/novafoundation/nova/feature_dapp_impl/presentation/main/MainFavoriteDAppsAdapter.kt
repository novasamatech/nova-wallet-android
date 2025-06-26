package io.novafoundation.nova.feature_dapp_impl.presentation.main

import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import io.novafoundation.nova.common.list.SingleItemAdapter
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_dapp_impl.databinding.ItemMainFavoriteDappsBinding
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DAppClickHandler
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel

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
        val binder = ItemMainFavoriteDappsBinding.inflate(parent.inflater(), parent, false)
        return FavoriteDAppHolder(binder, imageLoader, dappClickHandler, handler)
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
    private val binder: ItemMainFavoriteDappsBinding,
    imageLoader: ImageLoader,
    dAppClickHandler: DAppClickHandler,
    handler: MainFavoriteDAppsAdapter.Handler
) : RecyclerView.ViewHolder(binder.root) {

    private val favoritesAdapter = DappFavoritesAdapter(imageLoader, dAppClickHandler)

    init {
        binder.dAppMainFavoriteDAppList.adapter = favoritesAdapter
        binder.dAppMainFavoriteDAppsShow.setOnClickListener { handler.onManageFavoritesClick() }
    }

    fun bind(dapps: List<DappModel>) = with(binder) {
        dAppMainFavoriteDAppList.isGone = dapps.isEmpty()
        dAppMainFavoriteDAppTitle.isGone = dapps.isEmpty()
        dAppMainFavoriteDAppsShow.isGone = dapps.isEmpty()

        favoritesAdapter.submitList(dapps)
    }
}
