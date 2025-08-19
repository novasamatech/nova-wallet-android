package io.novafoundation.nova.feature_push_notifications.presentation.multisigs

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import kotlinx.parcelize.Parcelize

interface PushMultisigSettingsRequester : InterScreenRequester<PushMultisigSettingsRequester.Request, PushMultisigSettingsResponder.Response> {

    @Parcelize
    class Request(val isAtLeastOneMultisigWalletSelected: Boolean, val settings: PushMultisigSettingsModel) : Parcelable
}

interface PushMultisigSettingsResponder : InterScreenResponder<PushMultisigSettingsRequester.Request, PushMultisigSettingsResponder.Response> {

    @Parcelize
    class Response(val settings: PushMultisigSettingsModel) : Parcelable
}

interface PushMultisigSettingsCommunicator : PushMultisigSettingsRequester, PushMultisigSettingsResponder

