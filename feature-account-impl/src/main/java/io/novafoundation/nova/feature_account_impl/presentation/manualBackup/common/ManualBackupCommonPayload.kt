package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

sealed interface ManualBackupCommonPayload : Parcelable {

    val metaId: Long

    @Parcelize
    class DefaultAccount(
        override val metaId: Long
    ) : ManualBackupCommonPayload

    @Parcelize
    class ChainAccount(
        override val metaId: Long,
        val chainId: String
    ) : ManualBackupCommonPayload
}

fun ManualBackupCommonPayload.getChainIdOrNull(): String? {
    return if (this is ManualBackupCommonPayload.ChainAccount) {
        chainId
    } else {
        null
    }
}

fun ManualBackupCommonPayload.requireChainId(): String {
    return (this as ManualBackupCommonPayload.ChainAccount).chainId
}
