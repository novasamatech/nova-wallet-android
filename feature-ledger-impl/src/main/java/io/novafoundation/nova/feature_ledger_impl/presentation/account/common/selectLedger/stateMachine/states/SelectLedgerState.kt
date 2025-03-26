package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states

import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryMethods
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryRequirement
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.hasRequirement
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.isRequirementsNecessary
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.BluetoothDisabled
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.BluetoothEnabled
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.LocationDisabled
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.LocationEnabled
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect
import java.io.InvalidObjectException

sealed class SelectLedgerState : StateMachine.State<SelectLedgerState, SideEffect, SelectLedgerEvent> {

    protected suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.bluetoothDisabled(discoveryMethods: DiscoveryMethods) {
        if (!discoveryMethods.isBluetoothIsNecessary()) return

        missingDiscoveryState(setOf(DiscoveryRequirement.BLUETOOTH), discoveryMethods)
    }

    protected suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.locationDisabled(discoveryMethods: DiscoveryMethods) {
        if (!discoveryMethods.isLocationIsNecessary()) return

        missingDiscoveryState(setOf(DiscoveryRequirement.LOCATION), discoveryMethods)
    }

    protected suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.startDiscovery(discoveryMethods: DiscoveryMethods) {
        emitState(DiscoveringState(discoveryMethods))
        emitSideEffect(SideEffect.StartDiscovery)
    }

    protected suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.missingDiscoveryState(
        missingRequirements: Set<DiscoveryRequirement>,
        discoveryMethods: DiscoveryMethods
    ) {
        emitState(MissingDiscoveryRequirementState(missingRequirements, discoveryMethods))

        val sideEffect = getSideEffectFromRequirements(missingRequirements)
        emitSideEffect(sideEffect)
    }

    protected fun Set<DiscoveryRequirement>.updateByEvent(event: SelectLedgerEvent): Set<DiscoveryRequirement>? {
        return when (event) {
            LocationEnabled -> minus(DiscoveryRequirement.LOCATION)
            LocationDisabled -> plus(DiscoveryRequirement.LOCATION)
            BluetoothEnabled -> minus(DiscoveryRequirement.BLUETOOTH)
            BluetoothDisabled -> plus(DiscoveryRequirement.BLUETOOTH)
            else -> null
        }
    }

    protected fun DiscoveryMethods.isLocationIsNecessary() = isRequirementsNecessary() && hasRequirement(DiscoveryRequirement.LOCATION)

    protected fun DiscoveryMethods.isBluetoothIsNecessary() = isRequirementsNecessary() && hasRequirement(DiscoveryRequirement.LOCATION)
}

private fun getSideEffectFromRequirements(requirements: Set<DiscoveryRequirement>) = when {
    DiscoveryRequirement.BLUETOOTH in requirements -> SideEffect.EnableBluetooth
    DiscoveryRequirement.LOCATION in requirements -> SideEffect.EnableLocation
    else -> {
        val requirementsJoinedToString = requirements.joinToString { it.toString() }
        throw InvalidObjectException("missingRequirements contains values that aren't processing: $requirementsJoinedToString")
    }
}
