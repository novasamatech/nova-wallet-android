package io.novafoundation.nova.feature_account_api.domain.account.common

import io.novafoundation.nova.core.model.CryptoType

class EncryptionDefaults(
    val substrateCryptoType: CryptoType,
    val ethereumCryptoType: CryptoType,
    val substrateDerivationPath: String,
    val ethereumDerivationPath: String
)
