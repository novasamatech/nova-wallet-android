package io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.android.parcel.Parcelize

interface SelectAddressForTransactionRequester :
    InterScreenRequester<SelectAddressForTransactionRequester.Request, SelectAddressForTransactionResponder.Response> {

    @Parcelize
    class Request(
        val fromChainId: ChainId,
        val destinationChainId: ChainId,
        val selectedAddress: String?
    ) : Parcelable
}

interface SelectAddressForTransactionResponder :
    InterScreenResponder<SelectAddressForTransactionRequester.Request, SelectAddressForTransactionResponder.Response> {

    @Parcelize
    class Response(val selectedAddress: String) : Parcelable
}

interface SelectAddressCommunicator : SelectAddressForTransactionRequester, SelectAddressForTransactionResponder
