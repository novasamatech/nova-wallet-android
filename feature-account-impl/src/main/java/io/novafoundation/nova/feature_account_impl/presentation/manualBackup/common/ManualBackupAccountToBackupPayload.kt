package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

sealed interface ManualBackupAccountToBackupPayload : Parcelable {

    val metaId: Long

    @Parcelize
    class DefaultAccount(
        override val metaId: Long
    ) : ManualBackupAccountToBackupPayload

    @Parcelize
    class ChainAccount(
        override val metaId: Long,
        val chainId: String
    ) : ManualBackupAccountToBackupPayload
}
