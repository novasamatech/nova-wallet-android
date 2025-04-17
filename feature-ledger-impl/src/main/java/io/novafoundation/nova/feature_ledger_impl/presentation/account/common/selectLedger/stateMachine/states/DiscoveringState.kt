package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states

import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryMethods
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryRequirementAvailability
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.DiscoveredDevicesListChanged
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect

data class DiscoveringState(
    private val discoveryMethods: DiscoveryMethods,
    private val discoveryRequirementAvailability: DiscoveryRequirementAvailability,
    private val usedAllowedRequirementAvailabilityRequests: Boolean
) : SelectLedgerState() {

    companion object {

        fun initial(discoveryMethods: DiscoveryMethods, initialRequirementAvailability: DiscoveryRequirementAvailability): DiscoveringState {
            return DiscoveringState(discoveryMethods, initialRequirementAvailability, usedAllowedRequirementAvailabilityRequests = false)
        }
    }

    context(StateMachine.Transition<SelectLedgerState, SideEffect>)
    override suspend fun performTransition(event: SelectLedgerEvent) {
        when (event) {
            is SelectLedgerEvent.AvailabilityRequestsAllowed -> userAllowedAvailabilityRequests(
                discoveryMethods = discoveryMethods,
                discoveryRequirementAvailability = discoveryRequirementAvailability,
                nextState = copy(usedAllowedRequirementAvailabilityRequests = true)
            )

            is DiscoveredDevicesListChanged -> if (event.newDevices.isNotEmpty()) {
                val newState = DevicesFoundState(
                    devices = event.newDevices,
                    verifyingDevice = null,
                    discoveryMethods = discoveryMethods,
                    discoveryRequirementAvailability = discoveryRequirementAvailability,
                    usedAllowedRequirementAvailabilityRequests = usedAllowedRequirementAvailabilityRequests
                )
                emitState(newState)
            }

            else -> updateActiveDiscoveryMethodsByEvent(
                allDiscoveryMethods = discoveryMethods,
                previousRequirementsAvailability = discoveryRequirementAvailability,
                event = event,
                userAllowedRequirementAvailabilityRequests = usedAllowedRequirementAvailabilityRequests
            ) { newDiscoveryRequirementAvailability ->
                DiscoveringState(discoveryMethods, newDiscoveryRequirementAvailability, usedAllowedRequirementAvailabilityRequests)
            }
        }
    }

    context(StateMachine.Transition<SelectLedgerState, SideEffect>)
    override suspend fun bootstrap() {
        // Start discovery for all methods available from the start
        // I.e. those that do not have any requirements or already have all permissions / requirements satisfied
        updateActiveDiscoveryMethods(
            allDiscoveryMethods = discoveryMethods,
            previousActiveMethods = emptySet(),
            newRequirementsAvailability = discoveryRequirementAvailability
        )

        // Perform initial automatic requests to requirements and permissions if needed
        requestMissingDiscoveryRequirements(discoveryMethods, discoveryRequirementAvailability, usedAllowedRequirementAvailabilityRequests)
        requestPermissions(discoveryMethods, discoveryRequirementAvailability, usedAllowedRequirementAvailabilityRequests)
    }
}
