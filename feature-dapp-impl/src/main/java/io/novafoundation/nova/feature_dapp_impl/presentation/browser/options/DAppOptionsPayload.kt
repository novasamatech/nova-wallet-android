package io.novafoundation.nova.feature_dapp_impl.presentation.browser.options

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class DAppOptionsPayload(
    val isDesktopModeEnabled: Boolean
) : Parcelable
