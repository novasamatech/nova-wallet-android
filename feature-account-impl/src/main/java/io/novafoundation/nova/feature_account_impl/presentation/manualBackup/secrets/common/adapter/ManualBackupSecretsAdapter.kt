package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_account_impl.databinding.ItemManualBackupChainBinding
import io.novafoundation.nova.feature_account_impl.databinding.ItemManualBackupCryptoTypeBinding
import io.novafoundation.nova.feature_account_impl.databinding.ItemManualBackupJsonBinding
import io.novafoundation.nova.feature_account_impl.databinding.ItemManualBackupMnemonicBinding
import io.novafoundation.nova.feature_account_impl.databinding.ItemManualBackupSeedBinding
import io.novafoundation.nova.feature_account_impl.databinding.ItemManualBackupSubtitleBinding
import io.novafoundation.nova.feature_account_impl.databinding.ItemManualBackupTitleBinding
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
            TITLE_VIEW_TYPE -> ManualBackupTitleViewHolder(ItemManualBackupTitleBinding.inflate(parent.inflater(), parent, false))

            SUBTITLE_VIEW_TYPE -> ManualBackupSubtitleViewHolder(ItemManualBackupSubtitleBinding.inflate(parent.inflater(), parent, false))

            CHAIN_VIEW_TYPE -> ManualBackupChainViewHolder(
                ItemManualBackupChainBinding.inflate(parent.inflater(), parent, false)
            )

            MNEMONIC_VIEW_TYPE -> ManualBackupMnemonicViewHolder(
                ItemManualBackupMnemonicBinding.inflate(parent.inflater(), parent, false),
                itemHandler
            )

            SEED_VIEW_TYPE -> ManualBackupSeedViewHolder(
                ItemManualBackupSeedBinding.inflate(parent.inflater(), parent, false),
                itemHandler
            )

            JSON_VIEW_TYPE -> ManualBackupJsonViewHolder(
                ItemManualBackupJsonBinding.inflate(parent.inflater(), parent, false),
                itemHandler
            )

            CRYPTO_TYPE_VIEW_TYPE -> ManualBackupCryptoTypeViewHolder(ItemManualBackupCryptoTypeBinding.inflate(parent.inflater(), parent, false))

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
