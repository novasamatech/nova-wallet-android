package io.novafoundation.nova.feature_dapp_impl.web3.metamask.model

import android.os.Parcelable
import io.novafoundation.nova.feature_dapp_impl.web3.states.hostApi.ConfirmTxRequest
import kotlinx.android.parcel.Parcelize
import java.math.BigInteger

@Parcelize
class MetamaskTransaction(
    val gas: String?,
    val gasPrice: String?,
    val from: String,
    val to: String,
    val data: String,
    val value: BigInteger?
) : Parcelable

@Parcelize
class MetamaskSendTransactionRequest(override val id: String, val payload: Payload) : ConfirmTxRequest {

    @Parcelize
    class Payload(
        val transaction: MetamaskTransaction,
        val chain: MetamaskChain
    ) : Parcelable
}

typealias TransactionHash = String
