package io.novafoundation.nova.feature_account_impl.data.multisig.repository

import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.MultisigOperationsDao
import io.novafoundation.nova.core_db.model.MultisigOperationCallLocal
import io.novafoundation.nova.feature_account_api.data.multisig.repository.MultisigOperationLocalCallRepository
import io.novafoundation.nova.feature_account_api.domain.model.SavedMultisigOperationCall
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novasama.substrate_sdk_android.extensions.toHexString
import kotlinx.coroutines.flow.Flow

class RealMultisigOperationLocalCallRepository(
    private val multisigOperationsDao: MultisigOperationsDao
) : MultisigOperationLocalCallRepository {

    override suspend fun setMultisigCall(
        chainId: String,
        operationId: String,
        callHash: CallHash,
        call: String
    ) {
        multisigOperationsDao.insertOperation(
            MultisigOperationCallLocal(
                operationId = operationId,
                chainId = chainId,
                callHash = callHash.value.toHexString(),
                callInstance = call
            )
        )
    }

    override fun callsFlow(): Flow<List<SavedMultisigOperationCall>> {
        return multisigOperationsDao.observeOperations()
            .mapList {
                SavedMultisigOperationCall(
                    operationId = it.operationId,
                    chainId = it.chainId,
                    callHash = it.callHash,
                    callInstance = it.callInstance
                )
            }
    }
}
