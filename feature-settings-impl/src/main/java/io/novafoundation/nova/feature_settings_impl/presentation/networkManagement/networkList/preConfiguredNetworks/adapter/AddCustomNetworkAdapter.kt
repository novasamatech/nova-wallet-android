package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.preConfiguredNetworks.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.shape.getMaskedRipple
import io.novafoundation.nova.feature_settings_impl.R

class AddCustomNetworkAdapter(
    private val itemHandler: ItemHandler
) : RecyclerView.Adapter<AddCustomNetworkViewHolder>() {

    interface ItemHandler {

        fun onAddNetworkClicked()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddCustomNetworkViewHolder {
        return AddCustomNetworkViewHolder(parent, itemHandler)
    }

    override fun getItemCount(): Int {
        return 1
    }

    override fun onBindViewHolder(holder: AddCustomNetworkViewHolder, position: Int) {}
}

class AddCustomNetworkViewHolder(
    parent: ViewGroup,
    private val itemHandler: AddCustomNetworkAdapter.ItemHandler
) : ViewHolder(parent.inflateChild(R.layout.item_add_custom_network)) {

    init {
        itemView.background = itemView.context.getMaskedRipple(cornerSizeInDp = 0)
        itemView.setOnClickListener { itemHandler.onAddNetworkClicked() }
    }
}
