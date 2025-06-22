package io.novafoundation.nova.feature_account_api.domain.model

class SavedMultisigOperationCall(
    val operationId: String,
    val chainId: String,
    val callHash: String,
    val callInstance: String
)
