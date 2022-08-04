package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan.model

import android.os.Parcelable
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.ParitySignerSignInterScreenCommunicator
import kotlinx.android.parcel.Parcelize

@Parcelize
class ScanSignParitySignerPayload(
    val request: ParitySignerSignInterScreenCommunicator.Request,
): Parcelable
