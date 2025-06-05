package io.novafoundation.nova.feature_account_impl.data.events

import io.novafoundation.nova.common.utils.bus.BaseEventBus
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.events.allAffectedMetaAccountTypes
import io.novafoundation.nova.feature_account_api.data.externalAccounts.ExternalAccountsSyncService
import io.novafoundation.nova.feature_account_impl.data.cloudBackup.CloudBackupAccountsModificationsTracker

/**
 * Implementation of [MetaAccountChangesEventBus] that also performs some additional action known to account feature
 * Components from external modules can subscribe to this event bus on the upper level
 */
class RealMetaAccountChangesEventBus(
    private val externalAccountsSyncService: dagger.Lazy<ExternalAccountsSyncService>,
    private val cloudBackupAccountsModificationsTracker: CloudBackupAccountsModificationsTracker
) : BaseEventBus<MetaAccountChangesEventBus.Event>(), MetaAccountChangesEventBus {

    override suspend fun notify(event: MetaAccountChangesEventBus.Event, source: String?) {
        super.notify(event, source)

        cloudBackupAccountsModificationsTracker.recordAccountModified(event.allAffectedMetaAccountTypes())

        externalAccountsSyncService.get().syncOnAccountChange(source)
    }
}
