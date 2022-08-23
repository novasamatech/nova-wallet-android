package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states

import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.Event
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect

data class DevicesFoundState(
    val devices: List<LedgerDevice>,
    val connectingDevice: LedgerDevice?
) : SelectLedgerState() {

    override suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.performTransition(event: Event) {
        when(event) {
            Event.BluetoothDisabled -> bluetoothDisabled()

            is Event.ConnectionFailed -> connectingDevice?.let {
                emitState(copy(connectingDevice = null))
                emitSideEffect(SideEffect.PresentConnectionFailure(event.reason))
            }

            is Event.ConnectionSucceeded -> connectingDevice?.let {
                emitState(DeviceFlowStarted(devices = devices))
                emitSideEffect(SideEffect.StartDeviceFlow(connectingDevice, event.checkedAccount))
            }

            is Event.DeviceChosen -> {
                emitState(copy(connectingDevice = event.device))
                emitSideEffect(SideEffect.ConnectToDevice(event.device))
            }

            is Event.DiscoveredDevicesListChanged -> emitState(copy(devices = event.newDevices))

            else -> {}
        }
    }
}
