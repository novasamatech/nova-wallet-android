package io.novafoundation.nova.feature_account_api.domain.model

class SavedMultisigOperationCall(
    val metaId: Long,
    val chainId: String,
    val callHash: ByteArray,
    val callInstance: String
)
