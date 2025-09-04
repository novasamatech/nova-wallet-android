package io.novafoundation.nova.feature_account_api.data.proxy

import kotlinx.coroutines.flow.Flow

interface ProxySyncService {

    fun proxySyncTrigger(): Flow<*>

    fun startSyncing()

    suspend fun startSyncingSuspend()
}
