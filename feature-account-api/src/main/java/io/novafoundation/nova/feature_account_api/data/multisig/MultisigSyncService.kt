package io.novafoundation.nova.feature_account_api.data.multisig

import kotlinx.coroutines.flow.Flow

interface MultisigSyncService {

    fun startManualSync()

    fun automaticSync(): Flow<*>
}
