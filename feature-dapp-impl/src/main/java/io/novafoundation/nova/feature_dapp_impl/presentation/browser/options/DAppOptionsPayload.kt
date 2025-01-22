package io.novafoundation.nova.feature_dapp_impl.presentation.browser.options

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class DAppOptionsPayload(
    val isDesktopModeEnabled: Boolean
) : Parcelable
