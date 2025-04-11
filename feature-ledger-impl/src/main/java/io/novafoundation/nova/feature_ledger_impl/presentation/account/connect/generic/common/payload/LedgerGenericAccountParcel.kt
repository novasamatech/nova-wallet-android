package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.common.payload

import android.os.Parcelable
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novasama.substrate_sdk_android.encrypt.EncryptionType
import kotlinx.parcelize.Parcelize

@Parcelize
class LedgerGenericAccountParcel(
    val address: String,
    val publicKey: ByteArray,
    val encryptionType: EncryptionType,
    val derivationPath: String,
) : Parcelable

fun LedgerSubstrateAccount.toGenericParcel(): LedgerGenericAccountParcel {
    return LedgerGenericAccountParcel(address, publicKey, encryptionType, derivationPath)
}

fun LedgerGenericAccountParcel.toDomain(): LedgerSubstrateAccount {
    return LedgerSubstrateAccount(address, publicKey, encryptionType, derivationPath)
}
