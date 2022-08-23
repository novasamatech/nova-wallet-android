package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states

import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.Event
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect

sealed class SelectLedgerState : StateMachine.State<SelectLedgerState, SideEffect, Event> {

    protected suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.bluetoothDisabled() {
        emitState(NoBluetoothState())
        emitSideEffect(SideEffect.EnableBluetooth)
    }

    protected suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.startDiscovery() {
        emitState(DiscoveringState())
        emitSideEffect(SideEffect.StartDiscovery)
    }
}
