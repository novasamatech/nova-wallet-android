package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states

import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect

sealed class SelectLedgerState : StateMachine.State<SelectLedgerState, SideEffect, SelectLedgerEvent> {

    protected suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.bluetoothDisabled() {
        emitState(NoBluetoothState())
        emitSideEffect(SideEffect.EnableBluetooth)
    }

    protected suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.startDiscovery() {
        emitState(DiscoveringState())
        emitSideEffect(SideEffect.StartDiscovery)
    }
}
