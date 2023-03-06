package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states

import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.BluetoothDisabled
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.LocationDisabled
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.DeviceChosen
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.DiscoveredDevicesListChanged
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent.VerificationFailed
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect

data class DevicesFoundState(
    val devices: List<LedgerDevice>,
    val verifyingDevice: LedgerDevice?
) : SelectLedgerState() {

    override suspend fun StateMachine.Transition<SelectLedgerState, SideEffect>.performTransition(event: SelectLedgerEvent) {
        when (event) {
            BluetoothDisabled -> bluetoothDisabled()

            LocationDisabled -> locationDisabled()

            is VerificationFailed -> verifyingDevice?.let { device ->
                emitState(copy(verifyingDevice = null))
                emitSideEffect(SideEffect.PresentLedgerFailure(event.reason, device))
            }

            is SelectLedgerEvent.ConnectionVerified -> verifyingDevice?.let {
                emitState(DevicesFoundState(devices = devices, verifyingDevice = null))
            }

            is DeviceChosen -> {
                emitState(copy(verifyingDevice = event.device))
                emitSideEffect(SideEffect.VerifyConnection(event.device))
            }

            is DiscoveredDevicesListChanged -> emitState(copy(devices = event.newDevices))

            else -> {}
        }
    }
}
