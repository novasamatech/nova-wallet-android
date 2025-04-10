package io.novafoundation.nova.feature_dapp_impl.presentation.main

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedWalletModel
import io.novafoundation.nova.feature_dapp_impl.databinding.ItemDappHeaderBinding
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import io.novafoundation.nova.feature_dapp_impl.presentation.main.model.DAppCategoryModel

class DAppHeaderAdapter(
    val imageLoader: ImageLoader,
    val headerHandler: Handler,
    val categoriesHandler: DappCategoriesAdapter.Handler
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
            ItemDappHeaderBinding.inflate(parent.inflater(), parent, false),
            headerHandler,
            categoriesHandler
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

    override fun onBindViewHolder(holder: HeaderHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            payloads.filterIsInstance<Payload>().forEach {
                when (it) {
                    Payload.WALLET -> holder.bindWallet(walletModel)
                    Payload.CATEGORIES -> holder.bindCategories(categories)
                    Payload.CATEGORIES_SHIMMERING -> holder.bindCategoriesShimmering(showCategoriesShimmering)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return 1
    }

    fun setWallet(walletModel: SelectedWalletModel) {
        this.walletModel = walletModel
        notifyItemChanged(0, Payload.WALLET)
    }

    fun setCategories(categories: List<DAppCategoryModel>) {
        this.categories = categories
        notifyItemChanged(0, Payload.CATEGORIES)
    }

    fun showCategoriesShimmering(show: Boolean) {
        showCategoriesShimmering = show
        notifyItemChanged(0, Payload.CATEGORIES_SHIMMERING)
    }
}

class HeaderHolder(
    private val imageLoader: ImageLoader,
    private val binder: ItemDappHeaderBinding,
    headerHandler: DAppHeaderAdapter.Handler,
    categoriesHandler: DappCategoriesAdapter.Handler
) : RecyclerView.ViewHolder(binder.root) {

    private val categoriesAdapter = DappCategoriesAdapter(imageLoader, categoriesHandler)

    init {
        binder.dappMainSelectedWallet.setOnClickListener { headerHandler.onWalletClick() }
        binder.dappMainSearch.setOnClickListener { headerHandler.onSearchClick() }
        binder.dappMainManage.setOnClickListener { headerHandler.onManageClick() }
        binder.mainDappCategories.adapter = categoriesAdapter
    }

    fun bind(
        walletModel: SelectedWalletModel?,
        categoriesState: List<DAppCategoryModel>,
        favoritesDApps: List<DappModel>,
        showCategoriesShimmering: Boolean
    ) {
        bindWallet(walletModel)
        bindCategories(categoriesState)
        bindCategoriesShimmering(showCategoriesShimmering)
    }

    fun bindWallet(walletModel: SelectedWalletModel?) = with(binder) {
        walletModel?.let { dappMainSelectedWallet.setModel(walletModel) }
    }

    fun bindCategories(categoriesState: List<DAppCategoryModel>) = with(binder) {
        categoriesAdapter.submitList(categoriesState)
    }

    fun bindCategoriesShimmering(showCategoriesShimmering: Boolean) = with(itemView) {
        binder.categorizedDappsCategoriesShimmering.setVisible(showCategoriesShimmering, falseState = View.INVISIBLE)
        binder.mainDappCategories.isInvisible = showCategoriesShimmering
    }
}

private enum class Payload {
    WALLET, CATEGORIES, CATEGORIES_SHIMMERING
}
