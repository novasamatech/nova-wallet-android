package io.novafoundation.nova.feature_account_impl.presentation.seedScan

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import kotlinx.parcelize.Parcelize

interface ScanSeedRequester : InterScreenRequester<ScanSeedCommunicator.Request, ScanSeedCommunicator.Response>

interface ScanSeedResponder : InterScreenResponder<ScanSeedCommunicator.Request, ScanSeedCommunicator.Response>

interface ScanSeedCommunicator : ScanSeedRequester, ScanSeedResponder {

    @Parcelize
    class Request : Parcelable

    @Parcelize
    class Response(val secret: String) : Parcelable
}
