package io.novafoundation.nova.feature_account_impl.presentation.pincode

import android.os.Parcelable
import androidx.annotation.StringRes
import io.novafoundation.nova.common.navigation.DelayedNavigation
import io.novafoundation.nova.feature_account_impl.R
import kotlinx.android.parcel.Parcelize

@Parcelize
class ToolbarConfiguration(@StringRes val titleRes: Int? = null, val backVisible: Boolean = false) : Parcelable

sealed class PinCodeAction(open val toolbarConfiguration: ToolbarConfiguration) : Parcelable {

    @Parcelize
    class Create(val delayedNavigation: DelayedNavigation) :
        PinCodeAction(ToolbarConfiguration(R.string.pincode_title_create, false))

    @Parcelize
    open class Check(
        open val delayedNavigation: DelayedNavigation,
        override val toolbarConfiguration: ToolbarConfiguration
    ) : PinCodeAction(toolbarConfiguration)

    @Parcelize
    class CheckAfterInactivity(
        override val delayedNavigation: DelayedNavigation,
        override val toolbarConfiguration: ToolbarConfiguration
    ) : Check(delayedNavigation, toolbarConfiguration)

    @Parcelize
    object Change : PinCodeAction(ToolbarConfiguration(R.string.profile_pincode_change_title, true))

    @Parcelize
    class TwoFactorVerification(val useBiometryIfEnabled: Boolean = true) : PinCodeAction(ToolbarConfiguration(titleRes = null, backVisible = true))
}
