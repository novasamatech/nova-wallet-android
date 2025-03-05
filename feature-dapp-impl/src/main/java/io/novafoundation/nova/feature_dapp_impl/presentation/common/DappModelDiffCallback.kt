package io.novafoundation.nova.feature_dapp_impl.presentation.common

import androidx.recyclerview.widget.DiffUtil

object DappModelDiffCallback : DiffUtil.ItemCallback<DappModel>() {

    override fun areItemsTheSame(oldItem: DappModel, newItem: DappModel): Boolean {
        return oldItem.url == newItem.url
    }

    override fun areContentsTheSame(oldItem: DappModel, newItem: DappModel): Boolean {
        return oldItem == newItem
    }
}
