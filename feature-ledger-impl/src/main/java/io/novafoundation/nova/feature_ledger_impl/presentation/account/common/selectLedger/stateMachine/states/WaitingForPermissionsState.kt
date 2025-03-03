package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states

import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryMethod
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.PermissionsGranted
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect

class WaitingForPermissionsState(
    private val discoveryMethod: DiscoveryMethod,
    private val missingRequirements: Set<DiscoveryRequirement> = emptySet()
) : SelectLedgerState() {

    override suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.performTransition(event: SelectLedgerEvent) {
        when (event) {
            PermissionsGranted -> if (missingRequirements.isEmpty()) {
                startDiscovery(discoveryMethod)
            } else {
                missingDiscoveryState(missingRequirements, discoveryMethod)
            }

            else -> {
                val newMissingRequirements = missingRequirements.updateByEvent(event) ?: return
                if (newMissingRequirements.isNotEmpty()) {
                    emitState(WaitingForPermissionsState(discoveryMethod, newMissingRequirements))
                }
            }
        }
    }
}
