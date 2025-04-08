package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states

import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryMethods
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryRequirement
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect

class MissingDiscoveryRequirementState(
    private val missingRequirements: Set<DiscoveryRequirement>,
    private val discoveryMethods: DiscoveryMethods
) : SelectLedgerState() {

    override suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.performTransition(event: SelectLedgerEvent) {
        val newMissingRequirements = missingRequirements.updateByEvent(event) ?: return

        if (newMissingRequirements.isEmpty()) {
            startDiscovery(discoveryMethods)
        } else {
            missingDiscoveryState(newMissingRequirements, discoveryMethods)
        }
    }
}
