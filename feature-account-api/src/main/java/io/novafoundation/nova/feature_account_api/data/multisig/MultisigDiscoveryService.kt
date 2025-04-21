package io.novafoundation.nova.feature_account_api.data.multisig

import kotlinx.coroutines.flow.Flow

interface MultisigDiscoveryService {

    fun startManualAccountDiscoverySync()

    fun automaticAccountDiscoverySync(): Flow<*>
}
