package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupChainRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupChainViewHolder
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupCryptoTypeRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupCryptoTypeViewHolder
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupJsonRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupJsonViewHolder
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupMnemonicRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupMnemonicViewHolder
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupSeedRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupSeedViewHolder
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupSubtitleRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupSubtitleViewHolder
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupTitleRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupTitleViewHolder
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsViewHolder

private const val TITLE_VIEW_TYPE = 0
private const val SUBTITLE_VIEW_TYPE = 1
private const val CHAIN_VIEW_TYPE = 2
private const val MNEMONIC_VIEW_TYPE = 3
private const val SEED_VIEW_TYPE = 4
private const val JSON_VIEW_TYPE = 5
private const val CRYPTO_TYPE_VIEW_TYPE = 6

class ManualBackupSecretsAdapter(
    private val itemHandler: ManualBackupItemHandler
) : ListAdapter<ManualBackupSecretsRvItem, ManualBackupSecretsViewHolder>(ManualBackupDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManualBackupSecretsViewHolder {
        return when (viewType) {
            TITLE_VIEW_TYPE -> ManualBackupTitleViewHolder(parent.inflateChild(R.layout.item_manual_backup_title))
            SUBTITLE_VIEW_TYPE -> ManualBackupSubtitleViewHolder(parent.inflateChild(R.layout.item_manual_backup_subtitle))
            CHAIN_VIEW_TYPE -> ManualBackupChainViewHolder(parent.inflateChild(R.layout.item_manual_backup_chain))
            MNEMONIC_VIEW_TYPE -> ManualBackupMnemonicViewHolder(parent.inflateChild(R.layout.item_manual_backup_mnemonic), itemHandler)
            SEED_VIEW_TYPE -> ManualBackupSeedViewHolder(parent.inflateChild(R.layout.item_manual_backup_seed), itemHandler)
            JSON_VIEW_TYPE -> ManualBackupJsonViewHolder(parent.inflateChild(R.layout.item_manual_backup_json), itemHandler)
            CRYPTO_TYPE_VIEW_TYPE -> ManualBackupCryptoTypeViewHolder(parent.inflateChild(R.layout.item_manual_backup_crypto_type))
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(viewHolder: ManualBackupSecretsViewHolder, position: Int) {
        viewHolder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ManualBackupTitleRvItem -> TITLE_VIEW_TYPE
            is ManualBackupSubtitleRvItem -> SUBTITLE_VIEW_TYPE
            is ManualBackupChainRvItem -> CHAIN_VIEW_TYPE
            is ManualBackupMnemonicRvItem -> MNEMONIC_VIEW_TYPE
            is ManualBackupSeedRvItem -> SEED_VIEW_TYPE
            is ManualBackupJsonRvItem -> JSON_VIEW_TYPE
            is ManualBackupCryptoTypeRvItem -> CRYPTO_TYPE_VIEW_TYPE
            else -> throw IllegalArgumentException("Unknown item type")
        }
    }
}

object ManualBackupDiffCallback : DiffUtil.ItemCallback<ManualBackupSecretsRvItem>() {

    override fun areItemsTheSame(oldItem: ManualBackupSecretsRvItem, newItem: ManualBackupSecretsRvItem): Boolean {
        return oldItem.isItemTheSame(newItem)
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: ManualBackupSecretsRvItem, newItem: ManualBackupSecretsRvItem): Boolean {
        return oldItem == newItem
    }
}
