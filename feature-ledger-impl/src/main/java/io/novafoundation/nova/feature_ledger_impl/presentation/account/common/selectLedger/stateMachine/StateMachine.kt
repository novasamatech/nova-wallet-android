package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine

import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice

sealed class SideEffect {

    object EnableBluetooth : SideEffect()

    class PresentLedgerFailure(val reason: Throwable, val device: LedgerDevice) : SideEffect()

    class VerifyConnection(val device: LedgerDevice) : SideEffect()

    object StartDiscovery : SideEffect()
}

sealed class SelectLedgerEvent {

    class DiscoveredDevicesListChanged(val newDevices: List<LedgerDevice>) : SelectLedgerEvent()

    object BluetoothEnabled : SelectLedgerEvent()

    object BluetoothDisabled : SelectLedgerEvent()

    class DeviceChosen(val device: LedgerDevice) : SelectLedgerEvent()

    class VerificationFailed(val reason: Throwable) : SelectLedgerEvent()

    object ConnectionVerified : SelectLedgerEvent()

    object PermissionsGranted : SelectLedgerEvent()
}
