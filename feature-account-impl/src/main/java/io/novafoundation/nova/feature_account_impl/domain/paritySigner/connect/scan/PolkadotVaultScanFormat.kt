package io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.scan

import io.novafoundation.nova.feature_account_impl.domain.utils.ScanSecret
import io.novasama.substrate_sdk_android.encrypt.Sr25519
import io.novafoundation.nova.feature_account_impl.domain.utils.SecretQrFormat
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519SubstrateKeypairFactory
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.getPublicKeyFromSeed
import io.novasama.substrate_sdk_android.encrypt.qr.formats.SubstrateQrFormat
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.publicKeyToSubstrateAccountId
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId

class PolkadotVaultScanFormat(
    private val substrateQrFormat: SubstrateQrFormat = SubstrateQrFormat(),
    private val secretQrFormat: SecretQrFormat = SecretQrFormat()
) {
    fun decode(scanResult: String): Result<ParitySignerAccount> {
        return runCatching { publicFormat(scanResult) }
            .recoverCatching { secretFormat(scanResult) }
    }

    private fun publicFormat(scanResult: String): ParitySignerAccount.Public {
        val parsed = substrateQrFormat.decode(scanResult)
        return ParitySignerAccount.Public(parsed.address.toAccountId())
    }

    private fun secretFormat(scanResult: String): ParitySignerAccount.Secret {
        val parsed = secretQrFormat.decode(scanResult)
        val publicKey = when (val secret = parsed.secret) {
            is ScanSecret.EncryptedKeypair -> Sr25519.getPublicKeyFromSecret(secret.data)
            is ScanSecret.Seed -> Sr25519SubstrateKeypairFactory.getPublicKeyFromSeed(secret.data)
        }

        return ParitySignerAccount.Secret(
            publicKey.publicKeyToSubstrateAccountId(),
            parsed.secret
        )
    }
}
