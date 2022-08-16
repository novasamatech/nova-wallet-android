package io.novafoundation.nova.feature_account_api.presenatation.account.wallet

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.android.parcel.Parcelize


interface SelectWalletRequester : InterScreenRequester<SelectWalletRequester.Request, SelectWalletResponder.Response> {

    @Parcelize
    class Request(
        val chainId: ChainId,
        val chainAddress: String
    ) : Parcelable
}

interface SelectWalletResponder : InterScreenResponder<SelectWalletRequester.Request, SelectWalletResponder.Response> {

    @Parcelize
    class Response(
        val metaAccountId: Long
    ) : Parcelable
}


interface SelectWalletCommunicator : SelectWalletRequester, SelectWalletResponder
