package io.novafoundation.nova.feature_gift_impl.presentation.share

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class ShareGiftPayload(val giftId: Long, val isSecondOpen: Boolean) : Parcelable
