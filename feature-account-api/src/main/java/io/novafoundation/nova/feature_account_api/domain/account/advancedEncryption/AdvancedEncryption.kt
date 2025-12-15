package io.novafoundation.nova.feature_account_api.domain.account.advancedEncryption

import io.novafoundation.nova.common.utils.input.Input
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults

class AdvancedEncryptionInput(
    val substrateCryptoType: Input<CryptoType>,
    val substrateDerivationPath: Input<String>,
    val ethereumCryptoType: Input<CryptoType>,
    val ethereumDerivationPath: Input<String>
)

data class AdvancedEncryption(
    val substrateCryptoType: CryptoType?,
    val ethereumCryptoType: CryptoType?,
    val derivationPaths: DerivationPaths
) {

    companion object;

    data class DerivationPaths(
        val substrate: String?,
        val ethereum: String?
    ) {
        companion object {
            fun empty() = DerivationPaths(null, null)
        }
    }
}

fun EncryptionDefaults.recommended() = AdvancedEncryption(
    substrateCryptoType = substrateCryptoType,
    ethereumCryptoType = ethereumCryptoType,
    derivationPaths = AdvancedEncryption.DerivationPaths(
        substrate = substrateDerivationPath,
        ethereum = ethereumDerivationPath
    )
)

fun AdvancedEncryption.Companion.substrate(
    cryptoType: CryptoType,
    substrateDerivationPaths: String?
) = AdvancedEncryption(
    substrateCryptoType = cryptoType,
    ethereumCryptoType = null,
    derivationPaths = AdvancedEncryption.DerivationPaths(
        substrate = substrateDerivationPaths,
        ethereum = null
    )
)
