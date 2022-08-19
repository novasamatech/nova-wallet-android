package io.novafoundation.nova.feature_account_api.presenatation.account.wallet

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.android.parcel.Parcelize

interface SelectAddressRequester : InterScreenRequester<SelectAddressRequester.Request, SelectAddressResponder.Response> {

    @Parcelize
    class Request(
        val chainId: ChainId,
        val initialAddress: String?
    ) : Parcelable
}

interface SelectAddressResponder : InterScreenResponder<SelectAddressRequester.Request, SelectAddressResponder.Response> {

    @Parcelize
    class Response(val selectedAddress: String) : Parcelable
}

interface SelectAddressCommunicator : SelectAddressRequester, SelectAddressResponder
