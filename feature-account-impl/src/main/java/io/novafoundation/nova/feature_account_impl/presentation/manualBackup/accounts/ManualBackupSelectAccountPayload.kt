package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class ManualBackupSelectAccountPayload(
    val metaId: Long
) : Parcelable
