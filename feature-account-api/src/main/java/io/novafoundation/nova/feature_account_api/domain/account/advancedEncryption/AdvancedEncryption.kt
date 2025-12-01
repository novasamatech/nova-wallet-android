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

class AdvancedEncryption(
    val substrateCryptoType: CryptoType?,
    val ethereumCryptoType: CryptoType?,
    val derivationPaths: DerivationPaths
) {

    class DerivationPaths(
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
