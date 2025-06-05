package io.novafoundation.nova.feature_account_api.data.externalAccounts

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface ExternalAccountsSyncService {

    fun syncOnAccountChange(changeSource: String?)

    fun sync()

    fun sync(chain: Chain)
}
