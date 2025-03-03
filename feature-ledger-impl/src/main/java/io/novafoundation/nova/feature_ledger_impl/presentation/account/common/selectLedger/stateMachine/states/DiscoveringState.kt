package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states

import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryMethod
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.isBluetoothUsing
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.BluetoothDisabled
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.LocationDisabled
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.DiscoveredDevicesListChanged
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect

class DiscoveringState(private val discoveryMethod: DiscoveryMethod) : SelectLedgerState() {

    override suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.performTransition(event: SelectLedgerEvent) {
        when (event) {
            BluetoothDisabled -> if (discoveryMethod.isBluetoothUsing()) {
                bluetoothDisabled(discoveryMethod)
            }

            LocationDisabled -> if (discoveryMethod.isBluetoothUsing()) {
                locationDisabled(discoveryMethod)
            }

            is DiscoveredDevicesListChanged -> if (event.newDevices.isNotEmpty()) {
                val newState = DevicesFoundState(devices = event.newDevices, verifyingDevice = null, discoveryMethod)
                emitState(newState)
            }

            else -> {}
        }
    }
}
