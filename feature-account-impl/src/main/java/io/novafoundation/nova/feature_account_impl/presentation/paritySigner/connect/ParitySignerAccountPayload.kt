package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect

import android.os.Parcelable
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.scan.ParitySignerAccount
import kotlinx.android.parcel.Parcelize

@Parcelize
class ParitySignerAccountPayload(
    val accountId: ByteArray,
    val accountType: ParitySignerAccount.Type,
) : Parcelable

fun ParitySignerAccount(parcel: ParitySignerAccountPayload): ParitySignerAccount {
    return ParitySignerAccount(
        accountId = parcel.accountId,
        accountType = parcel.accountType
    )
}
