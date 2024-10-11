package io.novafoundation.nova.feature_dapp_impl.presentation.main

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedWalletModel
import io.novafoundation.nova.feature_dapp_impl.R

class DAppHeaderAdapter(val imageLoader: ImageLoader, val handler: Handler) : RecyclerView.Adapter<HeaderHolder>() {

    private var walletModel: SelectedWalletModel? = null

    interface Handler {
        fun onWalletClick()

        fun onSearchClick()

        fun onManageClick()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderHolder {
        return HeaderHolder(imageLoader, parent.inflateChild(R.layout.item_dapp_header), handler)
    }

    override fun onBindViewHolder(holder: HeaderHolder, position: Int) {
        holder.bind(walletModel)
    }

    override fun getItemCount(): Int {
        return 1
    }

    fun setWallet(walletModel: SelectedWalletModel) {
        this.walletModel = walletModel
        notifyItemChanged(0, true)
    }
}

class HeaderHolder(private val imageLoader: ImageLoader, view: View, handler: DAppHeaderAdapter.Handler) : RecyclerView.ViewHolder(view) {

    init {
        view.dappMainSelectedWallet.setOnClickListener { handler.onWalletClick() }
        view.dappMainSearch.setOnClickListener { handler.onSearchClick() }
        view.dappMainManage.setOnClickListener { handler.onManageClick() }
    }

    fun bind(walletModel: SelectedWalletModel?) {
        walletModel?.let { itemView.dappMainSelectedWallet.setModel(walletModel) }
    }
}
