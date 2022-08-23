package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states

import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.Event
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect

class WaitingForPermissionsState(private val bluetoothEnabled: Boolean = false): SelectLedgerState() {

    override suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.performTransition(event: Event) {
        when(event) {
            Event.PermissionsGranted -> if (bluetoothEnabled) {
                startDiscovery()
            } else {
                bluetoothDisabled()
            }

            Event.BluetoothDisabled -> WaitingForPermissionsState(bluetoothEnabled = false)
            Event.BluetoothEnabled -> WaitingForPermissionsState(bluetoothEnabled = true)

            else -> {}
        }
    }
}
