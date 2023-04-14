package io.novafoundation.nova.feature_dapp_impl.walletConnect

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import kotlinx.android.parcel.Parcelize

interface WalletConnectScanRequester : InterScreenRequester<WalletConnectScanCommunicator.Request, WalletConnectScanCommunicator.Response>

interface WalletConnectScanResponder : InterScreenResponder<WalletConnectScanCommunicator.Request, WalletConnectScanCommunicator.Response>

interface WalletConnectScanCommunicator : WalletConnectScanRequester, WalletConnectScanResponder {

    @Parcelize
    class Request : Parcelable

    @Parcelize
    class Response(val wcUri: String) : Parcelable
}
