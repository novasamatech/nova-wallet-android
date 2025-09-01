package io.novafoundation.nova.feature_account_impl.data.multisig.repository

import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.MultisigOperationsDao
import io.novafoundation.nova.core_db.model.MultisigOperationCallLocal
import io.novafoundation.nova.feature_account_api.data.multisig.repository.MultisigOperationLocalCallRepository
import io.novafoundation.nova.feature_account_api.domain.model.SavedMultisigOperationCall
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.toHexString
import kotlinx.coroutines.flow.Flow

class RealMultisigOperationLocalCallRepository(
    private val multisigOperationsDao: MultisigOperationsDao
) : MultisigOperationLocalCallRepository {

    override suspend fun setMultisigCall(operation: SavedMultisigOperationCall) {
        multisigOperationsDao.insertOperation(
            MultisigOperationCallLocal(
                chainId = operation.chainId,
                metaId = operation.metaId,
                callHash = operation.callHash.toHexString(),
                callInstance = operation.callInstance
            )
        )
    }

    override fun callsFlow(): Flow<List<SavedMultisigOperationCall>> {
        return multisigOperationsDao.observeOperations()
            .mapList {
                SavedMultisigOperationCall(
                    metaId = it.metaId,
                    chainId = it.chainId,
                    callHash = it.callHash.fromHex(),
                    callInstance = it.callInstance,
                )
            }
    }

    override suspend fun removeCallHashesExclude(metaId: Long, chainId: ChainId, excludedCallHashes: Set<CallHash>) {
        multisigOperationsDao.removeOperationsExclude(
            metaId,
            chainId,
            excludedCallHashes.map { it.value.toHexString() }
        )
    }
}
