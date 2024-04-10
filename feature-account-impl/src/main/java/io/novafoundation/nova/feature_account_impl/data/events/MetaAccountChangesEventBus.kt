package io.novafoundation.nova.feature_account_impl.data.events

import io.novafoundation.nova.common.utils.bus.BaseEventBus
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
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

        cloudBackupAccountsModificationsTracker.recordAccountModified(event.metaAccountType)

        when (event) {
            is MetaAccountChangesEventBus.Event.AccountAdded -> onAccountAdded(event.metaId, event.metaAccountType)
            is MetaAccountChangesEventBus.Event.AccountNameChanged -> onAccountNameChanged(event.metaId)
            is MetaAccountChangesEventBus.Event.AccountRemoved -> onAccountRemoved(event.metaId, event.metaAccountType)
            is MetaAccountChangesEventBus.Event.AccountStructureChanged -> onAccountStructureChanged(event.metaId, event.metaAccountType)
        }
    }

    private fun onAccountAdded(metaId: Long, type: LightMetaAccount.Type) {
        proxySyncService.get().startSyncingForMetaAccountChange(type)
    }

    private fun onAccountRemoved(metaId: Long, type: LightMetaAccount.Type) {}

    private fun onAccountStructureChanged(metaId: Long, type: LightMetaAccount.Type) {
       proxySyncService.get().startSyncingForMetaAccountChange(type)
    }

    private fun onAccountNameChanged(metaId: Long) {}

    private fun ProxySyncService.startSyncingForMetaAccountChange(accountType: LightMetaAccount.Type) {
        if (!accountType.isProxied) {
            startSyncing()
        }
    }
}
