package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedWalletModel
import io.novafoundation.nova.feature_staking_impl.databinding.ItemDashboardHeaderBinding

class DashboardHeaderAdapter(private val handler: Handler) : RecyclerView.Adapter<DashboardHeaderHolder>() {

    interface Handler {
        fun avatarClicked()
    }

    private var selectedWalletModel: SelectedWalletModel? = null

    fun setSelectedWallet(walletModel: SelectedWalletModel) {
        this.selectedWalletModel = walletModel

        notifyItemChanged(0, Payload.ADDRESS)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardHeaderHolder {
        return DashboardHeaderHolder(ItemDashboardHeaderBinding.inflate(parent.inflater(), parent, false), handler)
    }

    override fun onBindViewHolder(holder: DashboardHeaderHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            payloads.filterIsInstance<Payload>().forEach {
                when (it) {
                    Payload.ADDRESS -> holder.bindAddress(selectedWalletModel)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: DashboardHeaderHolder, position: Int) {
        holder.bind(selectedWalletModel)
    }

    override fun getItemCount(): Int {
        return 1
    }
}

private enum class Payload {
    ADDRESS
}

class DashboardHeaderHolder(
    private val binder: ItemDashboardHeaderBinding,
    handler: DashboardHeaderAdapter.Handler,
) : RecyclerView.ViewHolder(binder.root) {

    init {
        with(binder) {
            stakingDashboardHeaderAvatar.setOnClickListener { handler.avatarClicked() }
        }
    }

    fun bind(addressModel: SelectedWalletModel?) {
        bindAddress(addressModel)
    }

    fun bindAddress(walletModel: SelectedWalletModel?) = walletModel?.let {
        binder.stakingDashboardHeaderAvatar.setModel(it)
    }
}
