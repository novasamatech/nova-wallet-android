package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states

import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.Event
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect

class NoBluetoothState(): SelectLedgerState() {

    override suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.performTransition(event: Event) {
        when (event) {
            is Event.BluetoothEnabled -> startDiscovery()

            else -> {}
        }
    }
}
