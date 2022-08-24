package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states

import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.BluetoothDisabled
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.ConnectionFailed
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.ConnectionSucceeded
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.DeviceChosen
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.DiscoveredDevicesListChanged
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect

data class DevicesFoundState(
    val devices: List<LedgerDevice>,
    val connectingDevice: LedgerDevice?
) : SelectLedgerState() {

    override suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.performTransition(event: SelectLedgerEvent) {
        when (event) {
            BluetoothDisabled -> bluetoothDisabled()

            is ConnectionFailed -> connectingDevice?.let { device ->
                emitState(copy(connectingDevice = null))
                emitSideEffect(SideEffect.PresentConnectionFailure(event.reason, device))
            }

            is ConnectionSucceeded -> connectingDevice?.let {
                emitSideEffect(SideEffect.VerifyConnection(connectingDevice))
            }

            is SelectLedgerEvent.ConnectionVerified -> connectingDevice?.let {
                emitState(DeviceFlowStarted(devices = devices))
            }

            is DeviceChosen -> {
                emitState(copy(connectingDevice = event.device))
                emitSideEffect(SideEffect.ConnectToDevice(event.device))
            }

            is DiscoveredDevicesListChanged -> emitState(copy(devices = event.newDevices))

            else -> {}
        }
    }
}
