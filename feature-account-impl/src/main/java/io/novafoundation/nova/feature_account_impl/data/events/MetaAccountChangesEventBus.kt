package io.novafoundation.nova.feature_account_impl.data.events

import io.novafoundation.nova.common.utils.bus.BaseEventBus
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.events.allAffectedMetaAccountTypes
import io.novafoundation.nova.feature_account_api.data.events.collect
import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.domain.model.isProxied
import io.novafoundation.nova.feature_account_impl.data.cloudBackup.CloudBackupAccountsModificationsTracker

/**
 * Implementation of [MetaAccountChangesEventBus] that also performs some additional action known to account feature
 * Components from external modules can subscribe to this event bus on the upper level
 */
class RealMetaAccountChangesEventBus(
    private val proxySyncService: dagger.Lazy<ProxySyncService>,
    private val cloudBackupAccountsModificationsTracker: CloudBackupAccountsModificationsTracker,
) : BaseEventBus<MetaAccountChangesEventBus.Event>(), MetaAccountChangesEventBus {

    override suspend fun notify(event: MetaAccountChangesEventBus.Event) {
        super.notify(event)

        cloudBackupAccountsModificationsTracker.recordAccountModified(event.allAffectedMetaAccountTypes())

        if (event.shouldTriggerProxySync()) {
            proxySyncService.get().startSyncing()
        }
    }

    private fun MetaAccountChangesEventBus.Event.shouldTriggerProxySync(): Boolean {
        val potentialTriggers = collect(
            onAdd = { it.metaAccountType },
            onStructureChanged = { it.metaAccountType }
        )

        return potentialTriggers.any { !it.isProxied }
    }
}
