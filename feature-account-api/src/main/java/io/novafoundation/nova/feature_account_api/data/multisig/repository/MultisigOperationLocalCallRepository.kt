package io.novafoundation.nova.feature_account_api.data.multisig.repository

import io.novafoundation.nova.feature_account_api.domain.model.SavedMultisigOperationCall
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import kotlinx.coroutines.flow.Flow

interface MultisigOperationLocalCallRepository {

    suspend fun setMultisigCall(
        chainId: String,
        operationId: String,
        callHash: CallHash,
        call: String
    )

    fun callsFlow(): Flow<List<SavedMultisigOperationCall>>
}
