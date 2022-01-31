package io.novafoundation.nova.feature_wallet_impl.presentation.balance.list.view

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_wallet_impl.R
import io.novafoundation.nova.feature_wallet_impl.presentation.balance.list.model.TotalBalanceModel
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_asset_header.view.balanceListAvatar
import kotlinx.android.synthetic.main.item_asset_header.view.balanceListTotalBalance
import kotlinx.android.synthetic.main.item_asset_header.view.balanceListTotalTitle

class AssetsHeaderAdapter(private val handler: Handler) : RecyclerView.Adapter<HeaderHolder>() {

    interface Handler {

        fun avatarClicked()
    }

    private var totalBalance: TotalBalanceModel? = null
    private var addressModel: AddressModel? = null

    fun setTotalBalance(totalBalance: TotalBalanceModel) {
        this.totalBalance = totalBalance

        notifyItemChanged(0, Payload.TOTAL_BALANCE)
    }

    fun setAddress(addressModel: AddressModel) {
        this.addressModel = addressModel

        notifyItemChanged(0, Payload.ADDRESS)
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
                    Payload.ADDRESS -> holder.bindAddress(addressModel)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: HeaderHolder, position: Int) {
        holder.bind(totalBalance, addressModel)
    }

    override fun getItemCount(): Int {
        return 1
    }
}

private enum class Payload {
    TOTAL_BALANCE, ADDRESS
}

class HeaderHolder(
    override val containerView: View,
    handler: AssetsHeaderAdapter.Handler,
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        containerView.balanceListAvatar.setOnClickListener { handler.avatarClicked() }
    }

    fun bind(
        totalBalance: TotalBalanceModel?,
        addressModel: AddressModel?
    ) {
        bindTotalBalance(totalBalance)

        bindAddress(addressModel)
    }

    fun bindTotalBalance(totalBalance: TotalBalanceModel?) = totalBalance?.let {
        containerView.balanceListTotalBalance.showTotalBalance(totalBalance)
    }

    fun bindAddress(addressModel: AddressModel?) = addressModel?.let {
        containerView.balanceListTotalTitle.text = it.name
        containerView.balanceListAvatar.setImageDrawable(it.image)
    }
}
