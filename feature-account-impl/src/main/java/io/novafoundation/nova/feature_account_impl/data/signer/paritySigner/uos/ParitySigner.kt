package io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.uos

import jp.co.soramitsu.fearless_utils.encrypt.EncryptionType

enum class ParitySignerUOSContentCode(override val value: Byte) : UOS.UOSPreludeValue {

    SUBSTRATE(0x53),
}

enum class ParitySignerUOSPayloadCode(override val value: Byte) : UOS.UOSPreludeValue {

    TRANSACTION(0x02),
}

private const val ED25519_BYTE: Byte = 0x00
private const val SR25519_BYTE: Byte = 0x01
private const val ECDSA_BYTE: Byte = 0x02

fun EncryptionType.paritySignerUOSCryptoType(): UOS.UOSPreludeValue {
    val byte: Byte = when (this) {
        EncryptionType.ED25519 -> 0x00
        EncryptionType.SR25519 -> 0x01
        EncryptionType.ECDSA -> 0x02
    }

    return SimpleUOSPreludeValue(byte)
}

fun EncryptionType.Companion.fromParitySignerCryptoType(cryptoTypeByte: Byte): EncryptionType {
    return when(cryptoTypeByte) {
        ED25519_BYTE -> EncryptionType.ED25519
        SR25519_BYTE -> EncryptionType.SR25519
        ECDSA_BYTE -> EncryptionType.ECDSA
        else -> throw IllegalArgumentException("Unknown crypto type byte: $cryptoTypeByte")
    }
}
