package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.common.payload

import android.os.Parcelable
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerEvmAccount
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.parcelize.Parcelize

@Parcelize
class LedgerGenericEvmAccountParcel(
    val publicKey: ByteArray,
    val accountId: AccountId,
) : Parcelable

fun LedgerEvmAccount.toParcel(): LedgerGenericEvmAccountParcel {
    return LedgerGenericEvmAccountParcel(publicKey = publicKey, accountId = accountId)
}

fun LedgerGenericEvmAccountParcel.toDomain(): LedgerEvmAccount {
    return LedgerEvmAccount(accountId = accountId, publicKey = publicKey)
}
