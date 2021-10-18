package jp.co.soramitsu.feature_account_api.domain.model

import jp.co.soramitsu.core.model.CryptoType

class ImportJsonMetaData(
    val name: String?,
    val chainId: String?,
    val encryptionType: CryptoType
)
