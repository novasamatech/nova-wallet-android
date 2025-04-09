package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states

import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.common.utils.stateMachine.StateMachine.Transition
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryMethods
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryRequirement
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryRequirementAvailability
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.discoveryRequirements
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.filterBySatisfiedRequirements
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.permissionGranted
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.requirementMissing
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.requirementSatisfied
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect

sealed class SelectLedgerState : StateMachine.State<SelectLedgerState, SideEffect, SelectLedgerEvent> {

    context(Transition<SelectLedgerState, SideEffect>)
    protected suspend fun updateActiveDiscoveryMethodsByEvent(
        allDiscoveryMethods: DiscoveryMethods,
        previousRequirementsAvailability: DiscoveryRequirementAvailability,
        event: SelectLedgerEvent,
        userAllowedRequirementAvailabilityRequests: Boolean,
        newState: (newRequirementsAvailability: DiscoveryRequirementAvailability) -> SelectLedgerState
    ) {
        val newPreviousSatisfiedRequirements = updateDiscoveryRequirementAvailabilityByEvent(previousRequirementsAvailability, event) ?: return

        emitState(newState(newPreviousSatisfiedRequirements))

        updateActiveDiscoveryMethods(
            allDiscoveryMethods,
            previousRequirementsAvailability,
            newPreviousSatisfiedRequirements,
        )

        if (event is SelectLedgerEvent.DiscoveryRequirementMissing) {
            requestSatisfyDiscoveryRequirement(allDiscoveryMethods, event.requirement, userAllowedRequirementAvailabilityRequests)
        }
    }

    context(Transition<SelectLedgerState, SideEffect>)
    protected suspend fun userAllowedAvailabilityRequests(
        discoveryMethods: DiscoveryMethods,
        discoveryRequirementAvailability: DiscoveryRequirementAvailability,
        nextState: SelectLedgerState,
    ) {
        emitState(nextState)

        requestPermissions(discoveryMethods, discoveryRequirementAvailability, usedAllowedRequirementAvailabilityRequests = true)
        requestMissingDiscoveryRequirements(discoveryMethods, discoveryRequirementAvailability, usedAllowedRequirementAvailabilityRequests = true)
    }

    context(Transition<SelectLedgerState, SideEffect>)
    protected suspend fun updateActiveDiscoveryMethods(
        allDiscoveryMethods: DiscoveryMethods,
        previousRequirementsAvailability: DiscoveryRequirementAvailability,
        newRequirementsAvailability: DiscoveryRequirementAvailability,
    ) {
        val previousActiveDiscoveryMethods = allDiscoveryMethods.filterBySatisfiedRequirements(previousRequirementsAvailability)
        updateActiveDiscoveryMethods(
            allDiscoveryMethods,
            previousActiveDiscoveryMethods,
            newRequirementsAvailability,
        )
    }

    context(Transition<SelectLedgerState, SideEffect>)
    protected suspend fun updateActiveDiscoveryMethods(
        allDiscoveryMethods: DiscoveryMethods,
        previousActiveMethods: Set<DiscoveryMethods.Method>,
        newRequirementsAvailability: DiscoveryRequirementAvailability,
    ) {
        val newActiveMethods = allDiscoveryMethods.filterBySatisfiedRequirements(newRequirementsAvailability)

        val methodsToStart = newActiveMethods - previousActiveMethods
        val methodsToStop = previousActiveMethods - newActiveMethods

        methodsToStart.forEach { emitSideEffect(SideEffect.StartDiscovery(it)) }
        methodsToStop.forEach { emitSideEffect(SideEffect.StopDiscovery(it)) }
    }

    context(Transition<SelectLedgerState, SideEffect>)
    protected suspend fun requestPermissions(
        allDiscoveryMethods: DiscoveryMethods,
        newRequirementsAvailability: DiscoveryRequirementAvailability,
        usedAllowedRequirementAvailabilityRequests: Boolean
    ) {
        val allDiscoveryRequirements = allDiscoveryMethods.discoveryRequirements()

        // We can only request permissions if there is a single method or a user explicitly pressed a button to request permissions
        // Otherwise we don't bother them with the automatic requests as there might be method that do not require permissions at all
        val canRequestPermissions = allDiscoveryMethods.methods.size == 1 || usedAllowedRequirementAvailabilityRequests

        // We only need permissions when there is at least one requirement (assuming each requirement requires at least one permission)
        // and permissions has not been granted yet
        val permissionsNeeded = allDiscoveryRequirements.isNotEmpty() && !newRequirementsAvailability.permissionsGranted

        if (canRequestPermissions && permissionsNeeded) {
            val shouldExitUponDenial = allDiscoveryMethods.methods.size == 1

            emitSideEffect(SideEffect.RequestPermissions(allDiscoveryRequirements, shouldExitUponDenial))
        }
    }

    context(Transition<SelectLedgerState, SideEffect>)
    protected suspend fun requestMissingDiscoveryRequirements(
        discoveryMethods: DiscoveryMethods,
        discoveryRequirementAvailability: DiscoveryRequirementAvailability,
        usedAllowedRequirementAvailabilityRequests: Boolean
    ) {
        val canRequestRequirement = canRequestDiscoveryRequirements(discoveryMethods, usedAllowedRequirementAvailabilityRequests)
        if (!canRequestRequirement) return

        val allRequirements = discoveryMethods.discoveryRequirements()
        val missingRequirements = allRequirements - discoveryRequirementAvailability.satisfiedRequirements

        if (missingRequirements.isNotEmpty()) {
            emitSideEffect(SideEffect.RequestSatisfyRequirement(missingRequirements))
        }
    }

    context(Transition<SelectLedgerState, SideEffect>)
    private suspend fun requestSatisfyDiscoveryRequirement(
        allDiscoveryMethods: DiscoveryMethods,
        requirement: DiscoveryRequirement,
        usedAllowedRequirementAvailabilityRequests: Boolean
    ) {
        val canRequestRequirement = canRequestDiscoveryRequirements(allDiscoveryMethods, usedAllowedRequirementAvailabilityRequests)

        if (canRequestRequirement) {
            emitSideEffect(SideEffect.RequestSatisfyRequirement(listOf(requirement)))
        }
    }

    private fun canRequestDiscoveryRequirements(
        allDiscoveryMethods: DiscoveryMethods,
        usedAllowedRequirementAvailabilityRequests: Boolean
    ): Boolean {
        // We can only request permissions if there is a single method or a user explicitly pressed a button to request discovery requirements
        // Otherwise we don't bother them with the automatic requests as there might be method that do not require any requirements at all
        return allDiscoveryMethods.methods.size == 1 || usedAllowedRequirementAvailabilityRequests
    }

    private fun updateDiscoveryRequirementAvailabilityByEvent(
        availability: DiscoveryRequirementAvailability,
        event: SelectLedgerEvent
    ): DiscoveryRequirementAvailability? {
        return when (event) {
            is SelectLedgerEvent.DiscoveryRequirementMissing -> availability.requirementMissing(event.requirement)
            is SelectLedgerEvent.DiscoveryRequirementSatisfied -> availability.requirementSatisfied(event.requirement)
            is SelectLedgerEvent.PermissionsGranted -> availability.permissionGranted()
            else -> null
        }
    }
}
