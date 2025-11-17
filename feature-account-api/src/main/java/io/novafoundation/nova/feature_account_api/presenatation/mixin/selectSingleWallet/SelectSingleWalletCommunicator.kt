package io.novafoundation.nova.feature_account_api.presenatation.mixin.selectSingleWallet

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.feature_account_api.presenatation.mixin.common.SelectWalletFilterPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.parcelize.Parcelize

interface SelectSingleWalletRequester : InterScreenRequester<SelectSingleWalletRequester.Request, SelectSingleWalletResponder.Response> {

    @Parcelize
    class Request(
        val chainId: ChainId,
        val selectedMetaId: Long?,
        val filter: SelectWalletFilterPayload
    ) : Parcelable
}

interface SelectSingleWalletResponder : InterScreenResponder<SelectSingleWalletRequester.Request, SelectSingleWalletResponder.Response> {

    @Parcelize
    class Response(val metaId: Long) : Parcelable
}

interface SelectSingleWalletCommunicator : SelectSingleWalletRequester, SelectSingleWalletResponder
