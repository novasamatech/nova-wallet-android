package io.novafoundation.nova.feature_dapp_impl.web3.metamask.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class TypedMessage(
    val data: String,
    val raw: String?
) : Parcelable

typealias SignedMessage = String
