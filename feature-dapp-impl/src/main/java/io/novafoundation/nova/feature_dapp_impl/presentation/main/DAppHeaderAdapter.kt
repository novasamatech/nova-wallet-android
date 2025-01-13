package io.novafoundation.nova.feature_dapp_impl.presentation.main

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedWalletModel
import io.novafoundation.nova.feature_dapp_impl.databinding.ItemDappHeaderBinding

class DAppHeaderAdapter(val imageLoader: ImageLoader, val handler: Handler) : RecyclerView.Adapter<HeaderHolder>() {

    private var walletModel: SelectedWalletModel? = null

    interface Handler {
        fun onWalletClick()

        fun onSearchClick()

        fun onManageClick()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderHolder {
        return HeaderHolder(ItemDappHeaderBinding.inflate(parent.inflater(), parent, false), handler)
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

class HeaderHolder(private val binder: ItemDappHeaderBinding, handler: DAppHeaderAdapter.Handler) : RecyclerView.ViewHolder(binder.root) {

    init {
        binder.dappMainSelectedWallet.setOnClickListener { handler.onWalletClick() }
        binder.dappMainSearch.setOnClickListener { handler.onSearchClick() }
        binder.dappMainManage.setOnClickListener { handler.onManageClick() }
    }

    fun bind(walletModel: SelectedWalletModel?) {
        walletModel?.let { binder.dappMainSelectedWallet.setModel(walletModel) }
    }
}
