package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders

import android.graphics.Rect
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.decoration.ExtraSpaceViewHolder
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.feature_account_impl.databinding.ItemManualBackupTitleBinding
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsViewHolder

data class ManualBackupTitleRvItem(
    val title: String
) : ManualBackupSecretsRvItem {

    override fun isItemTheSame(other: ManualBackupSecretsRvItem): Boolean {
        return other is ManualBackupTitleRvItem && title == other.title
    }
}

class ManualBackupTitleViewHolder(private val binder: ItemManualBackupTitleBinding) : ManualBackupSecretsViewHolder(binder.root), ExtraSpaceViewHolder {

    override fun bind(item: ManualBackupSecretsRvItem) {
        require(item is ManualBackupTitleRvItem)
        binder.manualBackupSecretsTitle.text = item.title
    }

    override fun getExtraSpace(topViewHolder: RecyclerView.ViewHolder?, bottomViewHolder: RecyclerView.ViewHolder?): Rect? {
        if (topViewHolder == null || topViewHolder is ManualBackupChainViewHolder) {
            return null
        }

        return Rect(0, 16.dp(itemView.context), 0, 0)
    }
}
