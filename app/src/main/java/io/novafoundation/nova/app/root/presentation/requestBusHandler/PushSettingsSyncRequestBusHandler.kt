package io.novafoundation.nova.app.root.presentation.requestBusHandler

import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.events.collect
import io.novafoundation.nova.feature_account_api.data.events.takeMetaIdIfTypeMatches
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.SECRETS
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.PARITY_SIGNER
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.PROXIED
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.LEDGER
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.LEDGER_LEGACY
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.MULTISIG
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.WATCH_ONLY
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.POLKADOT_VAULT
import io.novafoundation.nova.feature_push_notifications.domain.interactor.PushNotificationsInteractor
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class PushSettingsSyncRequestBusHandler(
    private val scope: RootScope,
    private val metaAccountChangesEventBus: MetaAccountChangesEventBus,
    private val pushNotificationsInteractor: PushNotificationsInteractor
) : RequestBusHandler {

    override fun observe() {
        metaAccountChangesEventBus.observeEvent()
            .onEach { sourceEvent ->
                val changed = sourceEvent.event.collect(
                    onStructureChanged = { it.takeMetaIdIfTypeMatches(LightMetaAccount.Type::relevantToPushNotifications) }
                )
                val removed = sourceEvent.event.collect(
                    onRemoved = { it.takeMetaIdIfTypeMatches(LightMetaAccount.Type::relevantToPushNotifications) }
                )

                pushNotificationsInteractor.onMetaAccountChange(changed = changed, deleted = removed)
            }.launchIn(scope)
    }
}

// TODO multisig: this filter is probably only needed to avoid using batched accounts add via AddAccountRepository
// Currently, each proxy is added separately causing event bus to be triggered multiple times
// However this is wrong and we should rather setup proxy additions separately as someone might want to monitor proxy events as well
private fun LightMetaAccount.Type.relevantToPushNotifications(): Boolean {
    return when (this) {
        SECRETS,
        WATCH_ONLY,
        PARITY_SIGNER,
        LEDGER_LEGACY,
        LEDGER,
        POLKADOT_VAULT -> true

        PROXIED,
        MULTISIG -> false
    }
}
