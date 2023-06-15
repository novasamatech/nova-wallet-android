package io.novafoundation.nova.feature_assets.presentation.balance.list.view

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedWalletModel
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.NftPreviewUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.TotalBalanceModel
import io.novafoundation.nova.feature_wallet_connect_api.presentation.WalletConnectSessionsModel
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_asset_header.view.balanceListAssetPlaceholder
import kotlinx.android.synthetic.main.item_asset_header.view.balanceListAvatar
import kotlinx.android.synthetic.main.item_asset_header.view.balanceListFilters
import kotlinx.android.synthetic.main.item_asset_header.view.balanceListManage
import kotlinx.android.synthetic.main.item_asset_header.view.balanceListNfts
import kotlinx.android.synthetic.main.item_asset_header.view.balanceListSearch
import kotlinx.android.synthetic.main.item_asset_header.view.balanceListTotalBalance
import kotlinx.android.synthetic.main.item_asset_header.view.balanceListTotalTitle
import kotlinx.android.synthetic.main.item_asset_header.view.balanceListWalletConnect

class AssetsHeaderAdapter(private val handler: Handler) : RecyclerView.Adapter<HeaderHolder>() {

    interface Handler {
        fun totalBalanceClicked()

        fun searchClicked()
        fun filtersClicked()
        fun manageClicked()

        fun avatarClicked()

        fun goToNftsClicked()

        fun walletConnectClicked()
    }

    private var shouldShowPlaceholder: Boolean = false
    private var walletConnectModel: WalletConnectSessionsModel? = null
    private var totalBalance: TotalBalanceModel? = null
    private var selectedWalletModel: SelectedWalletModel? = null
    private var nftCountLabel: String? = null
    private var nftPreviews: List<NftPreviewUi>? = null

    fun setNftCountLabel(nftCount: String) {
        this.nftCountLabel = nftCount

        notifyItemChanged(0, Payload.NFT_COUNT)
    }

    fun setNftPreviews(previews: List<NftPreviewUi>) {
        this.nftPreviews = previews

        notifyItemChanged(0, Payload.NFT_PREVIEWS)
    }

    fun setTotalBalance(totalBalance: TotalBalanceModel) {
        this.totalBalance = totalBalance

        notifyItemChanged(0, Payload.TOTAL_BALANCE)
    }

    fun setSelectedWallet(walletModel: SelectedWalletModel) {
        this.selectedWalletModel = walletModel

        notifyItemChanged(0, Payload.ADDRESS)
    }

    fun setPlaceholderVisibility(shouldShowPlaceholder: Boolean) {
        this.shouldShowPlaceholder = shouldShowPlaceholder

        notifyItemChanged(0, Payload.PLACEHOLDER)
    }

    fun setWalletConnectModel(walletConnectModel: WalletConnectSessionsModel) {
        this.walletConnectModel = walletConnectModel

        notifyItemChanged(0, Payload.WALLET_CONNECT)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderHolder {
        return HeaderHolder(parent.inflateChild(R.layout.item_asset_header), handler)
    }

    override fun onBindViewHolder(holder: HeaderHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            payloads.filterIsInstance<Payload>().forEach {
                when (it) {
                    Payload.TOTAL_BALANCE -> holder.bindTotalBalance(totalBalance)
                    Payload.ADDRESS -> holder.bindAddress(selectedWalletModel)
                    Payload.NFT_COUNT -> holder.bindNftCount(nftCountLabel)
                    Payload.NFT_PREVIEWS -> holder.bindNftPreviews(nftPreviews)
                    Payload.PLACEHOLDER -> holder.bindPlaceholder(shouldShowPlaceholder)
                    Payload.WALLET_CONNECT -> holder.bindWalletConnect(walletConnectModel)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: HeaderHolder, position: Int) {
        holder.bind(totalBalance, selectedWalletModel, nftCountLabel, nftPreviews, shouldShowPlaceholder, walletConnectModel)
    }

    override fun getItemCount(): Int {
        return 1
    }
}

private enum class Payload {
    TOTAL_BALANCE, ADDRESS, NFT_COUNT, NFT_PREVIEWS, PLACEHOLDER, WALLET_CONNECT
}

class HeaderHolder(
    override val containerView: View,
    handler: AssetsHeaderAdapter.Handler,
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        with(containerView) {
            balanceListWalletConnect.setOnClickListener { handler.walletConnectClicked() }
            balanceListTotalBalance.setOnClickListener { handler.totalBalanceClicked() }
            balanceListManage.setOnClickListener { handler.manageClicked() }
            balanceListAvatar.setOnClickListener { handler.avatarClicked() }
            balanceListNfts.setOnClickListener { handler.goToNftsClicked() }
            balanceListSearch.setOnClickListener { handler.searchClicked() }
            balanceListFilters.setOnClickListener { handler.filtersClicked() }
        }
    }

    fun bind(
        totalBalance: TotalBalanceModel?,
        addressModel: SelectedWalletModel?,
        nftCount: String?,
        nftPreviews: List<NftPreviewUi>?,
        shouldShowPlaceholder: Boolean,
        walletConnect: WalletConnectSessionsModel?,
    ) {
        bindTotalBalance(totalBalance)
        bindAddress(addressModel)
        bindNftPreviews(nftPreviews)
        bindNftCount(nftCount)
        bindPlaceholder(shouldShowPlaceholder)
        bindWalletConnect(walletConnect)
    }

    fun bindNftPreviews(nftPreviews: List<NftPreviewUi>?) = with(containerView) {
        balanceListNfts.setPreviews(nftPreviews)
    }

    fun bindNftCount(nftCount: String?) = with(containerView) {
        balanceListNfts.setNftCount(nftCount)
    }

    fun bindTotalBalance(totalBalance: TotalBalanceModel?) = totalBalance?.let {
        with(containerView) {
            balanceListTotalBalance.showTotalBalance(totalBalance)
        }
    }

    fun bindAddress(walletModel: SelectedWalletModel?) = walletModel?.let {
        containerView.balanceListTotalTitle.text = it.name

        containerView.balanceListAvatar.setModel(it)
    }

    fun bindPlaceholder(shouldShowPlaceholder: Boolean) = with(containerView) {
        balanceListAssetPlaceholder.setVisible(shouldShowPlaceholder)
    }

    fun bindWalletConnect(walletConnectModel: WalletConnectSessionsModel?) = walletConnectModel?.let {
        containerView.balanceListWalletConnect.setConnectionCount(it.connections)
    }
}
