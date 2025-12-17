package io.novafoundation.nova.feature_push_notifications.presentation.settings

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class PushSettingsPayload(
    val enableSwitcherOnStart: Boolean,
    val navigation: InstantNavigation
) : Parcelable {
    companion object;

    sealed interface InstantNavigation : Parcelable {
        @Parcelize
        data object Nothing : InstantNavigation

        @Parcelize
        data object WithWalletSelection : InstantNavigation
    }
}

fun PushSettingsPayload.Companion.default(enableSwitcherOnStart: Boolean = false) =
    PushSettingsPayload(enableSwitcherOnStart, PushSettingsPayload.InstantNavigation.Nothing)

fun PushSettingsPayload.Companion.withWalletSelection(enableSwitcherOnStart: Boolean = false) =
    PushSettingsPayload(enableSwitcherOnStart, PushSettingsPayload.InstantNavigation.WithWalletSelection)
