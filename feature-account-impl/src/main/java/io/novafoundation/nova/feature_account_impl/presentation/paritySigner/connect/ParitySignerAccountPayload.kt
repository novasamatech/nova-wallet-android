package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class ParitySignerAccountPayload(
    val accountId: ByteArray,
): Parcelable
