package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states

import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect

class NoLocationState() : SelectLedgerState() {

    override suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.performTransition(event: SelectLedgerEvent) {
        when (event) {
            is SelectLedgerEvent.LocationEnabled -> {
                if (event.isBluetoothEnabled) {
                    startDiscovery()
                } else {
                    bluetoothDisabled()
                }
            }

            is SelectLedgerEvent.BluetoothDisabled -> {
                bluetoothDisabled()
            }

            else -> {}
        }
    }
}
