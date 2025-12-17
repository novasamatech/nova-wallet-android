package io.novafoundation.nova.feature_account_impl.domain.utils

import io.novasama.substrate_sdk_android.encrypt.qr.QrFormat
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.toHexString

private const val SECRET_PREFIX = "secret"
private const val SECRET_DELIMITER = ":"
private const val PARTS_MIN = 3
private const val PARTS_MAX = 4

class SecretQrFormat : QrFormat<SecretQrFormat.Payload> {

    class Payload(
        val secret: ScanSecret,
        val genesisHash: String,
        val name: String? = null
    )

    override fun encode(payload: Payload): String {
        val secretHex = payload.secret.data.toHexString(withPrefix = true)
        val genesisHash = payload.genesisHash

        val parts = listOfNotNull(
            SECRET_PREFIX,
            secretHex,
            genesisHash,
            payload.name
        )

        return parts.joinToString(SECRET_DELIMITER)
    }

    override fun decode(qrContent: String): Payload {
        val parts = qrContent.split(SECRET_DELIMITER)

        if (parts.size !in PARTS_MIN..PARTS_MAX) {
            throw QrFormat.InvalidFormatException("Invalid parts count: ${parts.size}")
        }

        val (prefix, secretEncoded, genesisHash) = parts

        if (prefix != SECRET_PREFIX) {
            throw QrFormat.InvalidFormatException("Wrong prefix: $prefix")
        }

        val secretBytes = secretEncoded.fromHex()

        val secret = when {
            secretBytes.isSubstrateSeed() -> ScanSecret.Seed(secretBytes)
            secretBytes.isSubstrateKeypair() -> ScanSecret.EncryptedKeypair(secretBytes)
            else -> throw QrFormat.InvalidFormatException("Invalid secret length: ${secretBytes.size}. Expected 32 or 64 bytes.")
        }

        val name = if (parts.size == PARTS_MAX) parts.last() else null

        return Payload(
            secret = secret,
            genesisHash = genesisHash,
            name = name
        )
    }
}
