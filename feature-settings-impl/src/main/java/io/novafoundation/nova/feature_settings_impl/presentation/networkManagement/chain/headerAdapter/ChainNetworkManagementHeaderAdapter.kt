package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.headerAdapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import io.novafoundation.nova.common.list.SingleItemAdapter
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_settings_impl.databinding.ItemChanNetworkManagementHeaderBinding

class ChainNetworkManagementHeaderAdapter(
    private val imageLoader: ImageLoader,
    private val itemHandler: ItemHandler
) : SingleItemAdapter<ChainNetworkManagementHeaderViewHolder>(isShownByDefault = true) {

    private var chainUiModel: ChainUi? = null
    private var chainEnabled = false
    private var autoBalanceEnabled = false
    private var networkCanBeDisabled = false

    interface ItemHandler {

        fun chainEnableClicked()

        fun autoBalanceClicked()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChainNetworkManagementHeaderViewHolder {
        return ChainNetworkManagementHeaderViewHolder(
            ItemChanNetworkManagementHeaderBinding.inflate(parent.inflater(), parent, false),
            imageLoader,
            itemHandler
        )
    }

    override fun onBindViewHolder(holder: ChainNetworkManagementHeaderViewHolder, position: Int) {
        chainUiModel?.let { holder.bind(it, chainEnabled, autoBalanceEnabled, networkCanBeDisabled) }
    }

    fun setChainUiModel(chainUiModel: ChainUi) {
        this.chainUiModel = chainUiModel
        notifyItemChanged(0)
    }

    fun setChainEnabled(chainEnabled: Boolean) {
        this.chainEnabled = chainEnabled
        notifyItemChanged(0)
    }

    fun setAutoBalanceEnabled(autoBalanceEnabled: Boolean) {
        this.autoBalanceEnabled = autoBalanceEnabled
        notifyItemChanged(0)
    }

    fun setNetworkCanBeDisabled(networkCanBeDisabled: Boolean) {
        this.networkCanBeDisabled = networkCanBeDisabled
    }
}

class ChainNetworkManagementHeaderViewHolder(
    private val binder: ItemChanNetworkManagementHeaderBinding,
    private val imageLoader: ImageLoader,
    private val itemHandler: ChainNetworkManagementHeaderAdapter.ItemHandler
) : RecyclerView.ViewHolder(binder.root) {

    init {
        with(binder) {
            chainNetworkManagementEnable.setOnClickListener { itemHandler.chainEnableClicked() }
            chainNetworkManagementAutoBalance.setOnClickListener { itemHandler.autoBalanceClicked() }
        }
    }

    fun bind(chainUi: ChainUi, chainEnabled: Boolean, autoBalanceEnabled: Boolean, networkCanBeDisabled: Boolean) {
        with(binder) {
            chainNetworkManagementIcon.loadChainIcon(chainUi.icon, imageLoader)
            chainNetworkManagementTitle.text = chainUi.name

            chainNetworkManagementEnable.setChecked(chainEnabled)
            chainNetworkManagementEnable.isEnabled = networkCanBeDisabled

            chainNetworkManagementAutoBalance.setChecked(autoBalanceEnabled)
            chainNetworkManagementAutoBalance.isEnabled = chainEnabled
        }
    }
}
