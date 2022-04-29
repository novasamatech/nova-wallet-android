package io.novafoundation.nova.feature_dapp_impl.web3.metamask.model

import android.os.Parcelable
import io.novafoundation.nova.feature_dapp_impl.web3.states.hostApi.ConfirmTxRequest
import kotlinx.android.parcel.Parcelize

@Parcelize
class MetamaskTransaction(
    val gas: String?,
    val gasPrice: String?,
    val from: String,
    val to: String,
    val data: String?,
    val value: String?
) : Parcelable

@Parcelize
class MetamaskSendTransactionRequest(override val id: String, val payload: Payload) : ConfirmTxRequest {

    sealed class Payload : Parcelable {

        abstract val chain: MetamaskChain

        abstract val originAddress: String

        @Parcelize
        class SendTx(
            val transaction: MetamaskTransaction,
            override val originAddress: String,
            override val chain: MetamaskChain
        ) : Payload()

        @Parcelize
        class SignTypedMessage(
            val message: TypedMessage,
            override val originAddress: String,
            override val chain: MetamaskChain
        ) : Payload()

        @Parcelize
        class PersonalSign(
            val message: PersonalSignMessage,
            override val originAddress: String,
            override val chain: MetamaskChain
        ) : Payload()
    }
}

typealias TransactionHash = String
