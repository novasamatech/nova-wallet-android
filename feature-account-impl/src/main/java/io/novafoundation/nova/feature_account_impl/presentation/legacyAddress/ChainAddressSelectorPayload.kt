package io.novafoundation.nova.feature_account_impl.presentation.legacyAddress

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class ChainAddressSelectorPayload(val chainId: String, val accountId: ByteArray) : Parcelable
