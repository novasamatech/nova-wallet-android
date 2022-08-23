package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine

import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice

sealed class SideEffect {

    object EnableBluetooth : SideEffect()

    class PresentConnectionFailure(val reason: Throwable) : SideEffect()

    class ConnectToDevice(val device: LedgerDevice) : SideEffect()

    class StartDeviceFlow(val device: LedgerDevice, val checkedAccount: LedgerSubstrateAccount) : SideEffect()

    object StartDiscovery: SideEffect()
}

sealed class Event {

    class DiscoveredDevicesListChanged(val newDevices: List<LedgerDevice>) : Event()

    object BluetoothEnabled : Event()

    object BluetoothDisabled : Event()

    class DeviceChosen(val device: LedgerDevice) : Event()

    class ConnectionSucceeded(val checkedAccount: LedgerSubstrateAccount) : Event()

    class ConnectionFailed(val reason: Throwable) : Event()

    object DeviceFlowCancelled : Event()

    object PermissionsGranted : Event()
}
