package io.novafoundation.nova.feature_assets.presentation.balance.list.view

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.presentation.masking.MaskableModel
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

        fun maskClicked()

        fun sendClicked()

        fun receiveClicked()

        fun buySellClicked()

        fun swapClicked()

        fun giftClicked()

        fun novaCardClick()

        fun pendingOperationsClicked()

        fun watchOnlyLearnMore()
    }

    private var filterIconRes: Int? = null
    private var walletConnectModel: WalletConnectSessionsModel? = null
    private var maskingEnabled: Boolean? = null
    private var totalBalance: TotalBalanceModel? = null
    private var selectedWalletModel: SelectedWalletModel? = null
    private var nftCountLabel: MaskableModel<String>? = null
    private var nftPreviews: MaskableModel<List<NftPreviewUi>>? = null
    private var pendingOperationsModel: PendingOperationsCountModel = PendingOperationsCountModel.Gone
    private var showWatchOnlyWarning: Boolean = false
    private var totalBalanceTitle: String? = null

    override fun getItemViewType(position: Int): Int {
        return AssetsHeaderHolder.viewType
    }

    fun setFilterIconRes(filterIconRes: Int) {
        this.filterIconRes = filterIconRes
    }

    fun setNftCountLabel(nftCount: MaskableModel<String>?) {
        this.nftCountLabel = nftCount

        notifyItemChanged(0, Payload.NFT_COUNT)
    }

    fun setNftPreviews(previews: MaskableModel<List<NftPreviewUi>>) {
        this.nftPreviews = previews

        notifyItemChanged(0, Payload.NFT_PREVIEWS)
    }

    fun setMaskingEnabled(maskingEnabled: Boolean) {
        this.maskingEnabled = maskingEnabled

        notifyItemChanged(0, Payload.MASKING_ENABLED)
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

    fun setTitleForTotalBalance(totalBalanceTitle: String) {
        this.totalBalanceTitle = totalBalanceTitle

        notifyItemChanged(0, Payload.TOTAL_BALANCE_TITLE)
    }

    fun setPendingOperationsCountModel(pendingOperationsCountModel: PendingOperationsCountModel) {
        this.pendingOperationsModel = pendingOperationsCountModel
        notifyItemChanged(0, Payload.PENDING_OPERATIONS_COUNT)
    }

    fun showWatchOnlyWarning(showWatchOnlyWarning: Boolean) {
        this.showWatchOnlyWarning = showWatchOnlyWarning
        notifyItemChanged(0, Payload.WATCH_ONLY_WARNING)
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
                    Payload.MASKING_ENABLED -> holder.bindMaskingEnabled(maskingEnabled)
                    Payload.ADDRESS -> holder.bindAddress(selectedWalletModel)
                    Payload.NFT_COUNT -> holder.bindNftCount(nftCountLabel)
                    Payload.NFT_PREVIEWS -> holder.bindNftPreviews(nftPreviews)
                    Payload.WALLET_CONNECT -> holder.bindWalletConnect(walletConnectModel)
                    Payload.PENDING_OPERATIONS_COUNT -> holder.bindPendingOperationsModel(pendingOperationsModel)
                    Payload.WATCH_ONLY_WARNING -> holder.bindPendingOperationsModel(pendingOperationsModel)
                    Payload.TOTAL_BALANCE_TITLE -> holder.bindTotalBalanceTitle(totalBalanceTitle)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: AssetsHeaderHolder, position: Int) {
        holder.bind(
            totalBalance,
            maskingEnabled,
            selectedWalletModel,
            nftCountLabel,
            nftPreviews,
            walletConnectModel,
            pendingOperationsModel,
            showWatchOnlyWarning,
            totalBalanceTitle
        )
    }

    override fun getItemCount(): Int {
        return 1
    }
}

private enum class Payload {
    TOTAL_BALANCE, MASKING_ENABLED, ADDRESS, NFT_COUNT, NFT_PREVIEWS, WALLET_CONNECT, PENDING_OPERATIONS_COUNT, WATCH_ONLY_WARNING, TOTAL_BALANCE_TITLE
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
            balanceListAvatar.setOnClickListener { handler.avatarClicked() }
            balanceListNfts.setOnClickListener { handler.goToNftsClicked() }
            balanceListTotalBalance.setOnClickListener { handler.totalBalanceClicked() }
            balanceListTotalBalance.onMaskingClick { handler.maskClicked() }
            balanceListTotalBalance.onSendClick { handler.sendClicked() }
            balanceListTotalBalance.onReceiveClick { handler.receiveClicked() }
            balanceListTotalBalance.onBuyClick { handler.buySellClicked() }
            balanceListTotalBalance.onGiftClick { handler.giftClicked() }
            balanceListNovaCard.setOnClickListener { handler.novaCardClick() }
            balanceListPendingOperations.setOnClickListener { handler.pendingOperationsClicked() }

            balanceListTotalBalance.onSwapClick { handler.swapClicked() }
            balanceListWarning.setOnLinkClickedListener { handler.watchOnlyLearnMore() }
        }
    }

    fun bind(
        totalBalance: TotalBalanceModel?,
        maskingEnabled: Boolean?,
        addressModel: SelectedWalletModel?,
        nftCount: MaskableModel<String>?,
        nftPreviews: MaskableModel<List<NftPreviewUi>>?,
        walletConnect: WalletConnectSessionsModel?,
        pendingOperationsCountModel: PendingOperationsCountModel,
        showWatchOnlyWarning: Boolean,
        totalBalanceTitle: String?
    ) {
        bindTotalBalance(totalBalance)
        bindMaskingEnabled(maskingEnabled)
        bindAddress(addressModel)
        bindNftPreviews(nftPreviews)
        bindNftCount(nftCount)
        bindWalletConnect(walletConnect)
        bindPendingOperationsModel(pendingOperationsCountModel)
        bindWatchOnlyWarning(showWatchOnlyWarning)
        bindTotalBalanceTitle(totalBalanceTitle)
    }

    fun bindNftPreviews(nftPreviews: MaskableModel<List<NftPreviewUi>>?) = with(viewBinding) {
        balanceListNfts.setPreviews(nftPreviews)
        viewBinding.balanceTableView.invalidateChildrenVisibility()
    }

    fun bindNftCount(nftCount: MaskableModel<String>?) = with(viewBinding) {
        balanceListNfts.setNftCount(nftCount)
    }

    fun bindMaskingEnabled(maskingEnabled: Boolean?) = maskingEnabled?.let {
        with(viewBinding) {
            balanceListTotalBalance.setMaskingEnabled(maskingEnabled)
        }
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
        viewBinding.balanceTableView.invalidateChildrenVisibility()
    }

    fun bindWatchOnlyWarning(showWatchOnlyWarning: Boolean) {
        viewBinding.balanceListWarning.isVisible = showWatchOnlyWarning
    }

    fun bindTotalBalanceTitle(totalBalanceTitle: String?) {
        viewBinding.balanceListTotalBalance.setTitle(totalBalanceTitle)
    }
}
