package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states

import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.Event
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect

class DiscoveringState : SelectLedgerState() {

    override suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.performTransition(event: Event) {
        when (event) {
            Event.BluetoothDisabled -> bluetoothDisabled()

            is Event.DiscoveredDevicesListChanged -> if (event.newDevices.isNotEmpty()) {
                val newState = DevicesFoundState(devices = event.newDevices, connectingDevice = null)
                emitState(newState)
            }

            else -> {}
        }
    }
}
