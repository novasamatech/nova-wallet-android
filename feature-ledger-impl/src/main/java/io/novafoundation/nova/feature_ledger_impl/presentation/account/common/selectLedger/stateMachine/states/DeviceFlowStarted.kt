package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states

import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.Event
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect

data class DeviceFlowStarted(
    val devices: List<LedgerDevice>,
    val bluetoothWasDisconnected: Boolean = false
): SelectLedgerState() {

    override suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.performTransition(event: Event) {
        when(event) {
            Event.BluetoothDisabled -> emitState(copy(bluetoothWasDisconnected = true))
            Event.BluetoothEnabled ->  emitState(copy(bluetoothWasDisconnected = false))

            Event.DeviceFlowCancelled -> if (bluetoothWasDisconnected) {
                bluetoothDisabled()
            } else {
                emitState(DevicesFoundState(devices, connectingDevice = null))
            }

            is Event.DiscoveredDevicesListChanged -> emitState(copy(devices = event.newDevices))

            else -> {}
        }
    }
}
