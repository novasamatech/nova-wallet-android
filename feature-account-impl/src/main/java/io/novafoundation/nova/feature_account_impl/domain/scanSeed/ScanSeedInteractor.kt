package io.novafoundation.nova.feature_account_impl.domain.scanSeed

import io.novafoundation.nova.feature_account_impl.domain.utils.ScanSecret
import io.novafoundation.nova.feature_account_impl.domain.utils.SecretQrFormat
import io.novasama.substrate_sdk_android.extensions.toHexString

interface ScanSeedInteractor {
    fun decodeSeed(content: String): Result<String>
}

class RealScanSeedInteractor(
    private val secretQrFormat: SecretQrFormat
) : ScanSeedInteractor {

    override fun decodeSeed(content: String): Result<String> = runCatching {
        val payload = secretQrFormat.decode(content)

        when (val secret = payload.secret) {
            is ScanSecret.EncryptedKeypair -> error("Encoded key can't be decoded to seed")
            is ScanSecret.Seed -> secret.data.toHexString(withPrefix = true)
        }
    }
}
