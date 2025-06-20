package io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.uos

import io.novafoundation.nova.core.model.CryptoType

enum class ParitySignerUOSContentCode(override val value: Byte) : UOS.UOSPreludeValue {

    SUBSTRATE(0x53),
}

enum class ParitySignerUOSPayloadCode(override val value: Byte) : UOS.UOSPreludeValue {

    TRANSACTION(0x02), MESSAGE(0x03), TRANSACTION_WITH_PROOF(0x06)
}

fun CryptoType.paritySignerUOSCryptoType(): UOS.UOSPreludeValue {
    val byte: Byte = when (this) {
        CryptoType.ED25519 -> 0x00
        CryptoType.SR25519 -> 0x01
        CryptoType.ECDSA -> 0x02
    }

    return SimpleUOSPreludeValue(byte)
}
