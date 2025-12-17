package io.novafoundation.nova.feature_gift_impl.presentation.claim

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class ClaimGiftPayload(val secret: ByteArray, val chainId: String, val assetId: Int) : Parcelable
