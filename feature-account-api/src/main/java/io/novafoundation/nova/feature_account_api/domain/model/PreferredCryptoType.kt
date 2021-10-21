package io.novafoundation.nova.feature_account_api.domain.model

import io.novafoundation.nova.core.model.CryptoType

data class PreferredCryptoType(
    val cryptoType: CryptoType,
    val frozen: Boolean
)
