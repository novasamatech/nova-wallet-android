package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.adapter.viewHolders

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.decoration.ExtraSpaceViewHolder
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.adapter.viewHolders.models.ManualBackupSecretsRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.adapter.viewHolders.models.ManualBackupSecretsViewHolder
import kotlinx.android.synthetic.main.item_manual_backup_subtitle.view.manualBackupSecretsSubtitle
import kotlinx.android.synthetic.main.item_manual_backup_title.view.manualBackupSecretsTitle

data class ManualBackupTitleRvItem(
    val title: String
) : ManualBackupSecretsRvItem {

    override fun isItemTheSame(other: ManualBackupSecretsRvItem): Boolean {
        return other is ManualBackupTitleRvItem && title == other.title
    }
}

class ManualBackupTitleViewHolder(itemView: View) : ManualBackupSecretsViewHolder(itemView), ExtraSpaceViewHolder {

    override fun bind(item: ManualBackupSecretsRvItem) {
        require(item is ManualBackupTitleRvItem)
        itemView.manualBackupSecretsTitle.text = item.title
    }

    override fun getExtraSpace(topViewHolder: RecyclerView.ViewHolder?, bottomViewHolder: RecyclerView.ViewHolder?): Rect? {
        if (topViewHolder == null || bottomViewHolder is ManualBackupChainViewHolder) {
            return Rect(0, 0, 0, 0)
        }

        return null
    }
}
