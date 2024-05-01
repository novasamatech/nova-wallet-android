package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts.adapter

import io.novafoundation.nova.common.utils.images.Icon

interface ManualBackupRVItem

class ManualBackupAccountGroupRVItem(
    val text: String
) : ManualBackupRVItem

class ManualBackupAccountRVItem(
    val chainId: String?, // It's null for default account
    val icon: Icon,
    val title: String,
    val subtitle: String?,
) : ManualBackupRVItem
