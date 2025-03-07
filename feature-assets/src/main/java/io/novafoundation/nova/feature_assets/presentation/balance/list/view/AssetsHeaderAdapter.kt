package io.novafoundation.nova.feature_assets.presentation.balance.list.view

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.recyclerView.asViewType
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedWalletModel
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.NftPreviewUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.TotalBalanceModel
import io.novafoundation.nova.feature_wallet_connect_api.presentation.WalletConnectSessionsModel
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_asset_header.view.balanceListAvatar
import kotlinx.android.synthetic.main.item_asset_header.view.balanceListNfts
import kotlinx.android.synthetic.main.item_asset_header.view.balanceListTotalBalance
import kotlinx.android.synthetic.main.item_asset_header.view.balanceListTotalTitle
import kotlinx.android.synthetic.main.item_asset_header.view.balanceListWalletConnect

class AssetsHeaderAdapter(private val handler: Handler) : RecyclerView.Adapter<AssetsHeaderHolder>() {

    companion object {
        const val VIEW_TYPE = 100

        fun getViewType() = VIEW_TYPE.asViewType()
    }

    interface Handler {
        fun totalBalanceClicked()

        fun searchClicked()

        fun manageClicked()

        fun avatarClicked()

        fun goToNftsClicked()

        fun walletConnectClicked()

        fun sendClicked()

        fun receiveClicked()

        fun buyClicked()

        fun swapClicked()
    }

    private var filterIconRes: Int? = null
    private var shouldShowPlaceholder: Boolean = false
    private var walletConnectModel: WalletConnectSessionsModel? = null
    private var totalBalance: TotalBalanceModel? = null
    private var selectedWalletModel: SelectedWalletModel? = null
    private var nftCountLabel: String? = null
    private var nftPreviews: List<NftPreviewUi>? = null

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE
    }

    fun setFilterIconRes(filterIconRes: Int) {
        this.filterIconRes = filterIconRes
    }

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

    fun setWalletConnectModel(walletConnectModel: WalletConnectSessionsModel) {
        this.walletConnectModel = walletConnectModel

        notifyItemChanged(0, Payload.WALLET_CONNECT)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetsHeaderHolder {
        return AssetsHeaderHolder(parent.inflateChild(R.layout.item_asset_header), handler)
    }

    override fun onBindViewHolder(holder: AssetsHeaderHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            payloads.filterIsInstance<Payload>().forEach {
                when (it) {
                    Payload.TOTAL_BALANCE -> holder.bindTotalBalance(totalBalance)
                    Payload.ADDRESS -> holder.bindAddress(selectedWalletModel)
                    Payload.NFT_COUNT -> holder.bindNftCount(nftCountLabel)
                    Payload.NFT_PREVIEWS -> holder.bindNftPreviews(nftPreviews)
                    Payload.WALLET_CONNECT -> holder.bindWalletConnect(walletConnectModel)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: AssetsHeaderHolder, position: Int) {
        holder.bind(
            totalBalance,
            selectedWalletModel,
            nftCountLabel,
            nftPreviews,
            walletConnectModel
        )
    }

    override fun getItemCount(): Int {
        return 1
    }
}

private enum class Payload {
    TOTAL_BALANCE, ADDRESS, NFT_COUNT, NFT_PREVIEWS, WALLET_CONNECT
}

class AssetsHeaderHolder(
    override val containerView: View,
    handler: AssetsHeaderAdapter.Handler,
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        with(containerView) {
            balanceListWalletConnect.setOnClickListener { handler.walletConnectClicked() }
            balanceListTotalBalance.setOnClickListener { handler.totalBalanceClicked() }
            balanceListAvatar.setOnClickListener { handler.avatarClicked() }
            balanceListNfts.setOnClickListener { handler.goToNftsClicked() }
            balanceListTotalBalance.onSendClick { handler.sendClicked() }
            balanceListTotalBalance.onReceiveClick { handler.receiveClicked() }
            balanceListTotalBalance.onBuyClick { handler.buyClicked() }
            balanceListTotalBalance.onSwapClick { handler.swapClicked() }
        }
    }

    fun bind(
        totalBalance: TotalBalanceModel?,
        addressModel: SelectedWalletModel?,
        nftCount: String?,
        nftPreviews: List<NftPreviewUi>?,
        walletConnect: WalletConnectSessionsModel?
    ) {
        bindTotalBalance(totalBalance)
        bindAddress(addressModel)
        bindNftPreviews(nftPreviews)
        bindNftCount(nftCount)
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

    fun bindWalletConnect(walletConnectModel: WalletConnectSessionsModel?) = walletConnectModel?.let {
        containerView.balanceListWalletConnect.setConnectionCount(it.connections)
    }
}
