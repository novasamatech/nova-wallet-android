package io.novafoundation.nova.feature_account_api.data.externalAccounts

import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus

interface ExternalAccountsSyncService {

    fun syncOnAccountChange(event: MetaAccountChangesEventBus.Event, changeSource: String?)

    fun sync()
}
