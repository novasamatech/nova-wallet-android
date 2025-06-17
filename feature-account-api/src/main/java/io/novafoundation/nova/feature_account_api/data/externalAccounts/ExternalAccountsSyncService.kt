package io.novafoundation.nova.feature_account_api.data.externalAccounts

import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface ExternalAccountsSyncService {

    fun syncOnAccountChange(event: MetaAccountChangesEventBus.Event, changeSource: String?)

    fun sync()

    fun sync(chain: Chain)
}
