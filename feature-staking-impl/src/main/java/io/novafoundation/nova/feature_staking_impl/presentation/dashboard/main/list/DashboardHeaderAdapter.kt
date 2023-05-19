package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.list

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedWalletModel
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_dashboard_header.view.stakingDashboardHeaderAvatar

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
        return DashboardHeaderHolder(parent.inflateChild(R.layout.item_dashboard_header), handler)
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
    override val containerView: View,
    handler: DashboardHeaderAdapter.Handler,
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        with(containerView) {
            stakingDashboardHeaderAvatar.setOnClickListener { handler.avatarClicked() }
        }
    }

    fun bind(addressModel: SelectedWalletModel?) {
        bindAddress(addressModel)
    }

    fun bindAddress(walletModel: SelectedWalletModel?) = walletModel?.let {
        containerView.stakingDashboardHeaderAvatar.setModel(it)
    }
}
