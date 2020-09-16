package jp.co.soramitsu.feature_account_impl.presentation.common.accountManagment

import jp.co.soramitsu.feature_account_impl.presentation.common.groupedList.BaseGroupedDiffCallback
import jp.co.soramitsu.feature_account_impl.presentation.view.advanced.network.model.NetworkModel

object AccountsDiffCallback : BaseGroupedDiffCallback<NetworkModel, AccountModel>(NetworkModel::class.java) {
    override fun areGroupItemsTheSame(oldItem: NetworkModel, newItem: NetworkModel): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areGroupContentsTheSame(oldItem: NetworkModel, newItem: NetworkModel): Boolean {
        return oldItem == newItem
    }

    override fun areChildItemsTheSame(oldItem: AccountModel, newItem: AccountModel): Boolean {
        return oldItem.address == newItem.address
    }

    override fun areChildContentsTheSame(oldItem: AccountModel, newItem: AccountModel): Boolean {
        return true
    }
}