package io.novafoundation.nova.core.model

enum class CryptoType {
    SR25519,
    ED25519,
    ECDSA;

    companion object
}

fun CryptoType.Companion.ethereumCryptoType(): CryptoType = CryptoType.ECDSA
