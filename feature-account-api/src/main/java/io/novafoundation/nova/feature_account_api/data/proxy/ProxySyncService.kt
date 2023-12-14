package io.novafoundation.nova.feature_account_api.data.proxy

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount

interface ProxySyncService {

    suspend fun startSyncing()

    suspend fun syncForMetaAccount(metaAccount: MetaAccount)
}
