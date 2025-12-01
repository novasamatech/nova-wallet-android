package io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.scan

import io.novafoundation.nova.common.utils.runAnotherOnFailure
import io.novasama.substrate_sdk_android.encrypt.Sr25519
import io.novasama.substrate_sdk_android.encrypt.qr.ScanSecret
import io.novasama.substrate_sdk_android.encrypt.qr.formats.SecretQrFormat
import io.novasama.substrate_sdk_android.encrypt.qr.formats.SubstrateQrFormat
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.publicKeyToSubstrateAccountId
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId

class PolkadotVaultScanFormat(
    private val substrateQrFormat: SubstrateQrFormat = SubstrateQrFormat(),
    private val secretQrFormat: SecretQrFormat = SecretQrFormat()
) {
    fun decode(scanResult: String): Result<ParitySignerAccount> {
        return runCatching { publicFormat(scanResult) }
            .runAnotherOnFailure { secretFormat(scanResult) }
    }

    private fun publicFormat(scanResult: String): ParitySignerAccount.Public {
        val parsed = substrateQrFormat.decode(scanResult)
        return ParitySignerAccount.Public(parsed.address.toAccountId())
    }

    private fun secretFormat(scanResult: String): ParitySignerAccount.Secret {
        val parsed = secretQrFormat.decode(scanResult)
        val publicKey = when (val secret = parsed.secret) {
            is ScanSecret.RawKey -> Sr25519.getPublicKeyFromSecret(secret.data)
            is ScanSecret.Seed -> Sr25519.getPublicKeyFromSeed(secret.data)
        }

        return ParitySignerAccount.Secret(
            publicKey.publicKeyToSubstrateAccountId(),
            parsed.secret
        )
    }
}
