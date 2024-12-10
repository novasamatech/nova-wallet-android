package io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet

import android.os.Parcelable
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator.Payload
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator.Response
import kotlinx.parcelize.Parcelize

interface SelectWalletRequester : InterScreenRequester<Payload, Response>

interface SelectWalletResponder : InterScreenResponder<Payload, Response>

interface SelectWalletCommunicator : SelectWalletRequester, SelectWalletResponder {

    @Parcelize
    class Payload(val currentMetaId: Long) : Parcelable

    @Parcelize
    class Response(val newMetaId: Long) : Parcelable
}
