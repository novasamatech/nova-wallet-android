package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts.adapter

import io.novafoundation.nova.common.utils.images.Icon

interface ManualBackupRvItem

class ManualBackupAccountGroupRvItem(
    val text: String
) : ManualBackupRvItem

class ManualBackupAccountRvItem(
    val chainId: String?, // It's null for default account
    val icon: Icon,
    val title: String,
    val subtitle: String?,
) : ManualBackupRvItem
