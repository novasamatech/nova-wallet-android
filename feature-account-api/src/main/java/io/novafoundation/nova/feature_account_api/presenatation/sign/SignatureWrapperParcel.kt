package io.novafoundation.nova.feature_account_api.presenatation.sign

import android.os.Parcelable
import io.novasama.substrate_sdk_android.encrypt.SignatureWrapper
import kotlinx.parcelize.Parcelize

sealed class SignatureWrapperParcel : Parcelable {

    @Parcelize
    class Ed25519(val signature: ByteArray) : SignatureWrapperParcel()

    @Parcelize
    class Sr25519(val signature: ByteArray) : SignatureWrapperParcel()

    @Parcelize
    class Ecdsa(
        val v: ByteArray,
        val r: ByteArray,
        val s: ByteArray
    ) : SignatureWrapperParcel()
}

fun SignatureWrapperParcel(signatureWrapper: SignatureWrapper): SignatureWrapperParcel {
    return with(signatureWrapper) {
        when (this) {
            is SignatureWrapper.Ed25519 -> SignatureWrapperParcel.Ed25519(signature)
            is SignatureWrapper.Sr25519 -> SignatureWrapperParcel.Sr25519(signature)
            is SignatureWrapper.Ecdsa -> SignatureWrapperParcel.Ecdsa(v, r, s)
        }
    }
}

fun SignatureWrapper(parcel: SignatureWrapperParcel): SignatureWrapper {
    return with(parcel) {
        when (this) {
            is SignatureWrapperParcel.Ed25519 -> SignatureWrapper.Ed25519(signature)
            is SignatureWrapperParcel.Sr25519 -> SignatureWrapper.Sr25519(signature)
            is SignatureWrapperParcel.Ecdsa -> SignatureWrapper.Ecdsa(v, r, s)
        }
    }
}
