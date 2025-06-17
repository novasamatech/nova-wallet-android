package io.novafoundation.nova.feature_assets.presentation.balance.list.view

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.recyclerView.WithViewType
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedWalletModel
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.ItemAssetHeaderBinding
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.NftPreviewUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.TotalBalanceModel
import io.novafoundation.nova.feature_wallet_connect_api.presentation.WalletConnectSessionsModel
import kotlinx.android.extensions.LayoutContainer

class AssetsHeaderAdapter(private val handler: Handler) : RecyclerView.Adapter<AssetsHeaderHolder>() {

    interface Handler {
        fun totalBalanceClicked()

        fun searchClicked()

        fun manageClicked()

        fun avatarClicked()

        fun goToNftsClicked()

        fun walletConnectClicked()

        fun sendClicked()

        fun receiveClicked()

        fun buySellClicked()

        fun swapClicked()

        fun novaCardClick()

        fun pendingOperationsClicked()
    }

    private var filterIconRes: Int? = null
    private var walletConnectModel: WalletConnectSessionsModel? = null
    private var totalBalance: TotalBalanceModel? = null
    private var selectedWalletModel: SelectedWalletModel? = null
    private var nftCountLabel: String? = null
    private var nftPreviews: List<NftPreviewUi>? = null
    private var pendingOperationsModel: PendingOperationsCountModel = PendingOperationsCountModel.Gone

    override fun getItemViewType(position: Int): Int {
        return AssetsHeaderHolder.viewType
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

    fun setPendingOperationsCountModel(pendingOperationsCountModel: PendingOperationsCountModel) {
        this.pendingOperationsModel = pendingOperationsCountModel
        notifyItemChanged(0, Payload.PENDING_OPERATIONS_COUNT)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetsHeaderHolder {
        return AssetsHeaderHolder(ItemAssetHeaderBinding.inflate(parent.inflater(), parent, false), handler)
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
                    Payload.PENDING_OPERATIONS_COUNT -> holder.bindPendingOperationsModel(pendingOperationsModel)
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
            walletConnectModel,
            pendingOperationsModel
        )
    }

    override fun getItemCount(): Int {
        return 1
    }
}

private enum class Payload {
    TOTAL_BALANCE, ADDRESS, NFT_COUNT, NFT_PREVIEWS, WALLET_CONNECT, PENDING_OPERATIONS_COUNT
}

class AssetsHeaderHolder(
    private val viewBinding: ItemAssetHeaderBinding,
    handler: AssetsHeaderAdapter.Handler,
) : RecyclerView.ViewHolder(viewBinding.root), LayoutContainer {

    override val containerView: View = viewBinding.root

    companion object : WithViewType {
        override val viewType: Int = R.layout.item_asset_header
    }

    init {
        with(viewBinding) {
            balanceListWalletConnect.setOnClickListener { handler.walletConnectClicked() }
            balanceListTotalBalance.setOnClickListener { handler.totalBalanceClicked() }
            balanceListAvatar.setOnClickListener { handler.avatarClicked() }
            balanceListNfts.setOnClickListener { handler.goToNftsClicked() }
            balanceListTotalBalance.onSendClick { handler.sendClicked() }
            balanceListTotalBalance.onReceiveClick { handler.receiveClicked() }
            balanceListTotalBalance.onBuyClick { handler.buySellClicked() }
            balanceListNovaCard.setOnClickListener { handler.novaCardClick() }
            balanceListPendingOperations.setOnClickListener { handler.pendingOperationsClicked() }

            balanceListTotalBalance.onSwapClick { handler.swapClicked() }
        }
    }

    fun bind(
        totalBalance: TotalBalanceModel?,
        addressModel: SelectedWalletModel?,
        nftCount: String?,
        nftPreviews: List<NftPreviewUi>?,
        walletConnect: WalletConnectSessionsModel?,
        pendingOperationsCountModel: PendingOperationsCountModel,
    ) {
        bindTotalBalance(totalBalance)
        bindAddress(addressModel)
        bindNftPreviews(nftPreviews)
        bindNftCount(nftCount)
        bindWalletConnect(walletConnect)
        bindPendingOperationsModel(pendingOperationsCountModel)
    }

    fun bindNftPreviews(nftPreviews: List<NftPreviewUi>?) = with(viewBinding) {
        balanceListNfts.setPreviews(nftPreviews)
    }

    fun bindNftCount(nftCount: String?) = with(viewBinding) {
        balanceListNfts.setNftCount(nftCount)
    }

    fun bindTotalBalance(totalBalance: TotalBalanceModel?) = totalBalance?.let {
        with(viewBinding) {
            balanceListTotalBalance.showTotalBalance(totalBalance)
        }
    }

    fun bindAddress(walletModel: SelectedWalletModel?) = walletModel?.let {
        viewBinding.balanceListTotalTitle.text = it.name

        viewBinding.balanceListAvatar.setModel(it)
    }

    fun bindWalletConnect(walletConnectModel: WalletConnectSessionsModel?) = walletConnectModel?.let {
        viewBinding.balanceListWalletConnect.setConnectionCount(it.connections)
    }

    fun bindPendingOperationsModel(model: PendingOperationsCountModel) {
        viewBinding.balanceListPendingOperations.setPendingOperationsCount(model)
    }
}
