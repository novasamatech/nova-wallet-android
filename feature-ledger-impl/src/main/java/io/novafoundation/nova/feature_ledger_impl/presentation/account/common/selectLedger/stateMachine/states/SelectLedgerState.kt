package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states

import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryMethod
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.isBluetoothRequired
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.BluetoothDisabled
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.BluetoothEnabled
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.LocationDisabled
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.LocationEnabled
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect
import java.io.InvalidObjectException

sealed class SelectLedgerState : StateMachine.State<SelectLedgerState, SideEffect, SelectLedgerEvent> {

    protected suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.bluetoothDisabled(discoveryMethod: DiscoveryMethod) {
        missingDiscoveryState(setOf(DiscoveryRequirement.BLUETOOTH), discoveryMethod)
    }

    protected suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.locationDisabled(discoveryMethod: DiscoveryMethod) {
        missingDiscoveryState(setOf(DiscoveryRequirement.LOCATION), discoveryMethod)
    }

    protected suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.startDiscovery(discoveryMethod: DiscoveryMethod) {
        emitState(DiscoveringState(discoveryMethod))
        emitSideEffect(SideEffect.StartDiscovery)
    }

    protected suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.missingDiscoveryState(
        missingRequirements: Set<DiscoveryRequirement>,
        discoveryMethod: DiscoveryMethod
    ) {
        if (discoveryMethod.isBluetoothRequired()) {
            val sideEffect = getSideEffectFromRequirements(missingRequirements)

            emitState(MissingDiscoveryRequirementState(missingRequirements, discoveryMethod))

            emitSideEffect(sideEffect)
        }
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
}

private fun getSideEffectFromRequirements(requirements: Set<DiscoveryRequirement>) = when {
    DiscoveryRequirement.BLUETOOTH in requirements -> SideEffect.EnableBluetooth
    DiscoveryRequirement.LOCATION in requirements -> SideEffect.EnableLocation
    else -> {
        val requirementsJoinedToString = requirements.joinToString { it.toString() }
        throw InvalidObjectException("missingRequirements contains values that aren't processing: $requirementsJoinedToString")
    }
}
