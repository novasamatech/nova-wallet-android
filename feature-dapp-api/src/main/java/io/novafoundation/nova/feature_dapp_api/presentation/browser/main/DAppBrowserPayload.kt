package io.novafoundation.nova.feature_dapp_api.presentation.browser.main

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

interface DAppBrowserPayload : Parcelable {

    @Parcelize
    class Tab(val id: String) : DAppBrowserPayload

    @Parcelize
    class Address(val address: String) : DAppBrowserPayload
}
