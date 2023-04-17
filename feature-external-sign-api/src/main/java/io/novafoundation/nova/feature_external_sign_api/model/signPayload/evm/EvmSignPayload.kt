package io.novafoundation.nova.feature_external_sign_api.model.signPayload.evm

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

sealed class EvmSignPayload : Parcelable {

    abstract val originAddress: String

    @Parcelize
    class ConfirmTx(
        val transaction: EvmTransaction,
        override val originAddress: String,
        val chainSource: EvmChainSource,
        val action: Action,
    ) : EvmSignPayload() {

        enum class Action {
            SIGN, SEND
        }
    }

    @Parcelize
    class SignTypedMessage(
        val message: EvmTypedMessage,
        override val originAddress: String,
    ) : EvmSignPayload()

    @Parcelize
    class PersonalSign(
        val message: EvmPersonalSignMessage,
        override val originAddress: String,
    ) : EvmSignPayload()
}

@Parcelize
class EvmChainSource(val evmChainId: Int, val unknownChainOptions: UnknownChainOptions) : Parcelable {

    sealed class UnknownChainOptions : Parcelable {

        @Parcelize
        object MustBeKnown : UnknownChainOptions()

        @Parcelize
        class WithFallBack(val evmChain: EvmChain) : UnknownChainOptions()
    }
}
