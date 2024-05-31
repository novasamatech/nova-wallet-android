package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.adapter.items

import android.view.View
import androidx.recyclerview.widget.RecyclerView

interface NetworkListRvItem {
    val id: String
}

abstract class NetworkListViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    abstract fun bind(item: NetworkListRvItem)
}
