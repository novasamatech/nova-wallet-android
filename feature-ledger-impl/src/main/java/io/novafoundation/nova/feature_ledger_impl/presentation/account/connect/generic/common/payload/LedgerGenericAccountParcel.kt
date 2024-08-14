package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.common.payload

import android.os.Parcelable
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novasama.substrate_sdk_android.encrypt.EncryptionType
import kotlinx.android.parcel.Parcelize

@Parcelize
class LedgerGenericAccountParcel(
    val index: Int,
    val address: String,
    val publicKey: ByteArray,
    val encryptionType: EncryptionType,
    val derivationPath: String,
) : Parcelable

fun LedgerSubstrateAccount.toGenericParcel(): LedgerGenericAccountParcel {
    return LedgerGenericAccountParcel(index, address, publicKey, encryptionType, derivationPath)
}

fun LedgerGenericAccountParcel.toDomain(): LedgerSubstrateAccount {
    return LedgerSubstrateAccount(index, address, publicKey, encryptionType, derivationPath)
}
