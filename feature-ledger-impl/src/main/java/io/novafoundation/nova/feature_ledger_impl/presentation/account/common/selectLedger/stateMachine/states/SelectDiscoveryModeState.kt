package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states

import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.isRequirementsNecessary
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect

class SelectDiscoveryModeState : SelectLedgerState() {

    override suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.performTransition(event: SelectLedgerEvent) {
        if (event is SelectLedgerEvent.DiscoveryMethodSelected) {
            when {
                event.discoveryMethods.isRequirementsNecessary() -> emitState(WaitingForPermissionsState(event.discoveryMethods))

                else -> startDiscovery(event.discoveryMethods)
            }
        }
    }
}
