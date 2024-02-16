package io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import kotlinx.android.parcel.Parcelize

interface SelectMultipleWalletsRequester : InterScreenRequester<SelectMultipleWalletsRequester.Request, SelectMultipleWalletsResponder.Response> {

    @Parcelize
    class Request(
        val titleText: String,
        val max: Int,
        val currentlySelectedMetaIds: Set<Long>
    ) : Parcelable
}

interface SelectMultipleWalletsResponder : InterScreenResponder<SelectMultipleWalletsRequester.Request, SelectMultipleWalletsResponder.Response> {

    @Parcelize
    class Response(val selectedMetaIds: Set<Long>) : Parcelable
}

interface SelectMultipleWalletsCommunicator : SelectMultipleWalletsRequester, SelectMultipleWalletsResponder
