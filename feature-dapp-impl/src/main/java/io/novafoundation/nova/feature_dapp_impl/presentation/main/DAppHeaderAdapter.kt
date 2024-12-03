package io.novafoundation.nova.feature_dapp_impl.presentation.main

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedWalletModel
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.presentation.main.model.DAppCategoryModel
import kotlinx.android.synthetic.main.item_dapp_header.view.categorizedDappsCategories
import kotlinx.android.synthetic.main.item_dapp_header.view.categorizedDappsCategoriesShimmering
import kotlinx.android.synthetic.main.item_dapp_header.view.dappMainManage
import kotlinx.android.synthetic.main.item_dapp_header.view.dappMainSearch
import kotlinx.android.synthetic.main.item_dapp_header.view.dappMainSelectedWallet

class DAppHeaderAdapter(
    val imageLoader: ImageLoader,
    val headerHandler: Handler,
    val categoriesHandler: DappCategoriesAdapter.Handler
) : RecyclerView.Adapter<HeaderHolder>() {

    private var walletModel: SelectedWalletModel? = null
    private var categories: List<DAppCategoryModel>? = null
    private var showCategoriesShimmering: Boolean = false

    interface Handler {
        fun onWalletClick()

        fun onSearchClick()

        fun onManageClick()

        fun onCategoryClicked(id: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderHolder {
        return HeaderHolder(imageLoader, parent.inflateChild(R.layout.item_dapp_header), headerHandler, categoriesHandler)
    }

    override fun onBindViewHolder(holder: HeaderHolder, position: Int) {
        holder.bind(walletModel, categories, showCategoriesShimmering)
    }

    override fun getItemCount(): Int {
        return 1
    }

    fun setWallet(walletModel: SelectedWalletModel) {
        this.walletModel = walletModel
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
    categoriesHandler: DappCategoriesAdapter.Handler
) : RecyclerView.ViewHolder(view) {

    private val categoriesAdapter = DappCategoriesAdapter(imageLoader, categoriesHandler)

    init {
        view.dappMainSelectedWallet.setOnClickListener { headerHandler.onWalletClick() }
        view.dappMainSearch.setOnClickListener { headerHandler.onSearchClick() }
        view.dappMainManage.setOnClickListener { headerHandler.onManageClick() }
        view.categorizedDappsCategories.adapter = categoriesAdapter
    }

    fun bind(walletModel: SelectedWalletModel?, categoriesState: List<DAppCategoryModel>?, showCategoriesShimmering: Boolean) {
        walletModel?.let { itemView.dappMainSelectedWallet.setModel(walletModel) }
        categoriesAdapter.submitList(categoriesState)
        itemView.categorizedDappsCategoriesShimmering.isVisible = showCategoriesShimmering
        itemView.categorizedDappsCategories.isGone = showCategoriesShimmering
    }
}
