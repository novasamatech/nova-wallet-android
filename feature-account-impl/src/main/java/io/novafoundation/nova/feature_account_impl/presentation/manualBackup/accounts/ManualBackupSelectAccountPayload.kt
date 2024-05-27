package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class ManualBackupSelectAccountPayload(
    val metaId: Long
) : Parcelable
