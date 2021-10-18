package jp.co.soramitsu.feature_account_api.domain.model

import jp.co.soramitsu.core.model.CryptoType

data class PreferredCryptoType(
    val cryptoType: CryptoType,
    val frozen: Boolean
)
