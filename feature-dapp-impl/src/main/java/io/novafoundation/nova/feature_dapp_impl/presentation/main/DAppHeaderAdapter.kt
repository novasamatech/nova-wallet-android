package io.novafoundation.nova.feature_dapp_impl.presentation.main

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedWalletModel
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DAppClickHandler
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import io.novafoundation.nova.feature_dapp_impl.presentation.main.model.DAppCategoryModel
import kotlinx.android.synthetic.main.item_dapp_header.view.mainDappCategories
import kotlinx.android.synthetic.main.item_dapp_header.view.categorizedDappsCategoriesShimmering
import kotlinx.android.synthetic.main.item_dapp_header.view.dAppMainFavoriteDAppList
import kotlinx.android.synthetic.main.item_dapp_header.view.dAppMainFavoriteDAppTitle
import kotlinx.android.synthetic.main.item_dapp_header.view.dAppMainFavoriteDAppsShow
import kotlinx.android.synthetic.main.item_dapp_header.view.dappMainManage
import kotlinx.android.synthetic.main.item_dapp_header.view.dappMainSearch
import kotlinx.android.synthetic.main.item_dapp_header.view.dappMainSelectedWallet

class DAppHeaderAdapter(
    val imageLoader: ImageLoader,
    val headerHandler: Handler,
    val categoriesHandler: DappCategoriesAdapter.Handler,
    val dAppClickHandler: DAppClickHandler
) : RecyclerView.Adapter<HeaderHolder>() {

    private var walletModel: SelectedWalletModel? = null
    private var categories: List<DAppCategoryModel> = emptyList()
    private var favoritesDApps: List<DappModel> = emptyList()
    private var showCategoriesShimmering: Boolean = false

    interface Handler {
        fun onWalletClick()

        fun onSearchClick()

        fun onManageClick()

        fun onManageFavoritesClick()

        fun onCategoryClicked(id: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderHolder {
        return HeaderHolder(
            imageLoader,
            parent.inflateChild(R.layout.item_dapp_header),
            headerHandler,
            categoriesHandler,
            dAppClickHandler
        )
    }

    override fun onBindViewHolder(holder: HeaderHolder, position: Int) {
        holder.bind(
            walletModel,
            categories,
            favoritesDApps,
            showCategoriesShimmering
        )
    }

    override fun getItemCount(): Int {
        return 1
    }

    fun setWallet(walletModel: SelectedWalletModel) {
        this.walletModel = walletModel
        notifyItemChanged(0, true)
    }

    fun setFavorites(favoritesDApps: List<DappModel>) {
        this.favoritesDApps = favoritesDApps
        notifyItemChanged(0, true)
    }

    fun setCategories(categories: List<DAppCategoryModel>) {
        this.categories = categories
        notifyItemChanged(0, true)
    }

    fun showCategoriesShimmering(show: Boolean) {
        showCategoriesShimmering = show
        notifyItemChanged(0, true)
    }
}

class HeaderHolder(
    imageLoader: ImageLoader,
    view: View,
    headerHandler: DAppHeaderAdapter.Handler,
    categoriesHandler: DappCategoriesAdapter.Handler,
    dAppClickHandler: DAppClickHandler
) : RecyclerView.ViewHolder(view) {

    private val categoriesAdapter = DappCategoriesAdapter(imageLoader, categoriesHandler)
    private val favoritesAdapter = DappFavoritesAdapter(imageLoader, dAppClickHandler)

    init {
        view.dappMainSelectedWallet.setOnClickListener { headerHandler.onWalletClick() }
        view.dappMainSearch.setOnClickListener { headerHandler.onSearchClick() }
        view.dappMainManage.setOnClickListener { headerHandler.onManageClick() }
        view.mainDappCategories.adapter = categoriesAdapter
        view.dAppMainFavoriteDAppList.adapter = favoritesAdapter
        view.dAppMainFavoriteDAppsShow.setOnClickListener { headerHandler.onManageFavoritesClick() }
    }

    fun bind(
        walletModel: SelectedWalletModel?,
        categoriesState: List<DAppCategoryModel>,
        favoritesDApps: List<DappModel>,
        showCategoriesShimmering: Boolean
    ) = with(itemView) {
        walletModel?.let { dappMainSelectedWallet.setModel(walletModel) }
        categoriesAdapter.submitList(categoriesState)
        categorizedDappsCategoriesShimmering.setVisible(showCategoriesShimmering, falseState = View.INVISIBLE)
        mainDappCategories.isInvisible = showCategoriesShimmering

        favoritesAdapter.submitList(favoritesDApps)
        dAppMainFavoriteDAppList.isGone = favoritesDApps.isEmpty()
        dAppMainFavoriteDAppTitle.isGone = favoritesDApps.isEmpty()
        dAppMainFavoriteDAppsShow.isGone = favoritesDApps.isEmpty()
    }
}
