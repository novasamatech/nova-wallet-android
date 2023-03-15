package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states

import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.BluetoothDisabled
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.BluetoothEnabled
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.LocationDisabled
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.LocationEnabled
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect
import java.io.InvalidObjectException

sealed class SelectLedgerState : StateMachine.State<SelectLedgerState, SideEffect, SelectLedgerEvent> {

    protected suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.bluetoothDisabled() {
        missingDiscoveryState(setOf(DiscoveryRequirement.BLUETOOTH))
    }

    protected suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.locationDisabled() {
        missingDiscoveryState(setOf(DiscoveryRequirement.LOCATION))
    }

    protected suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.startDiscovery() {
        emitState(DiscoveringState())
        emitSideEffect(SideEffect.StartDiscovery)
    }

    protected suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.missingDiscoveryState(
        missingRequirements: Set<DiscoveryRequirement>
    ) {
        val sideEffect = when {
            DiscoveryRequirement.BLUETOOTH in missingRequirements -> SideEffect.EnableBluetooth
            DiscoveryRequirement.LOCATION in missingRequirements -> SideEffect.EnableLocation
            else -> {
                val requirementsJoinedToString = missingRequirements.joinToString { it.toString() }
                throw InvalidObjectException("missingRequirements contains values that aren't processing: $requirementsJoinedToString")
            }
        }
        emitState(MissingDiscoveryRequirementState(missingRequirements))
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
}
