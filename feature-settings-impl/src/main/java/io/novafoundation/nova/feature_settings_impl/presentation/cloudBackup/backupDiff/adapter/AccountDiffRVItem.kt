package io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.backupDiff.adapter

import android.graphics.drawable.Drawable
import io.novafoundation.nova.common.view.ChipLabelModel

interface CloudBackupDiffRVItem

class CloudBackupDiffGroupRVItem(
    val chipModel: ChipLabelModel
) : CloudBackupDiffRVItem

class AccountDiffRVItem(
    val id: String,
    val icon: Drawable,
    val title: String,
    val state: String,
    val stateColorRes: Int,
    val stateIconRes: Int?,
) : CloudBackupDiffRVItem
