package io.novafoundation.nova.feature_account_api.domain.account.common

import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class EncryptionDefaults(
    val substrateCryptoType: CryptoType,
    val ethereumCryptoType: CryptoType,
    val substrateDerivationPath: String,
    val ethereumDerivationPath: String
)

class ChainEncryptionDefaults(
    val cryptoType: CryptoType,
    val derivationPath: String
)

fun EncryptionDefaults.forChain(chain: Chain): ChainEncryptionDefaults {
    return if (chain.isEthereumBased) {
        ChainEncryptionDefaults(
            cryptoType = ethereumCryptoType,
            derivationPath = ethereumDerivationPath
        )
    } else {
        ChainEncryptionDefaults(
            cryptoType = substrateCryptoType,
            derivationPath = substrateDerivationPath
        )
    }
}
