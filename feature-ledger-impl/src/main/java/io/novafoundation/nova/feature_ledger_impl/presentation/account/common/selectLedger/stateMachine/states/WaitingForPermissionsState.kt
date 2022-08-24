package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states

import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.BluetoothDisabled
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.BluetoothEnabled
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.PermissionsGranted
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect

class WaitingForPermissionsState(private val bluetoothEnabled: Boolean = false): SelectLedgerState() {

    override suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.performTransition(event: SelectLedgerEvent) {
        when(event) {
            PermissionsGranted -> if (bluetoothEnabled) {
                startDiscovery()
            } else {
                bluetoothDisabled()
            }

            BluetoothDisabled -> WaitingForPermissionsState(bluetoothEnabled = false)
            BluetoothEnabled -> WaitingForPermissionsState(bluetoothEnabled = true)

            else -> {}
        }
    }
}
