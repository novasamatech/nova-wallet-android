package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.adapter.viewHolders.models

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class ManualBackupSecretsViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    abstract fun bind(item: ManualBackupSecretsRvItem)
}
