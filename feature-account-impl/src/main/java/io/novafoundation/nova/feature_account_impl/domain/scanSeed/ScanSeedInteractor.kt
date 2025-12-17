package io.novafoundation.nova.feature_account_impl.domain.scanSeed

import io.novafoundation.nova.feature_account_impl.domain.utils.SecretQrFormat
import io.novasama.substrate_sdk_android.extensions.toHexString

interface ScanSeedInteractor {
    fun decodeSeed(content: String): Result<String>
}

class RealScanSeedInteractor(
    private val secretQrFormat: SecretQrFormat
) : ScanSeedInteractor {

    override fun decodeSeed(content: String): Result<String> = runCatching {
        secretQrFormat.decode(content)
            .secret
            .data
            .toHexString(withPrefix = true)
    }
}
