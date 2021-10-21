package io.novafoundation.nova.feature_account_api.domain.model

import io.novafoundation.nova.core.model.CryptoType

class ImportJsonMetaData(
    val name: String?,
    val chainId: String?,
    val encryptionType: CryptoType
)
