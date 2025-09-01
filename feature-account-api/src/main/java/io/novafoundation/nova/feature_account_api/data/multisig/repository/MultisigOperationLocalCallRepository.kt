package io.novafoundation.nova.feature_account_api.data.multisig.repository

import io.novafoundation.nova.feature_account_api.domain.model.SavedMultisigOperationCall
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow

interface MultisigOperationLocalCallRepository {

    suspend fun setMultisigCall(operation: SavedMultisigOperationCall)

    fun callsFlow(): Flow<List<SavedMultisigOperationCall>>

    suspend fun removeCallHashesExclude(metaId: Long, chainId: ChainId, excludedCallHashes: Set<CallHash>)
}
