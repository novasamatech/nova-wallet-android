package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger

import android.Manifest
import android.os.Build
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.findDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.performDiscovery
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.model.SelectLedgerModel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.Event
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states.DeviceFlowStarted
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states.DevicesFoundState
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states.DiscoveringState
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states.NoBluetoothState
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states.SelectLedgerState
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states.WaitingForPermissionsState
import io.novafoundation.nova.feature_ledger_impl.sdk.discovery.ble.BleScanFailed
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

enum class BluetoothState {
    ON, OFF
}

abstract class SelectLedgerViewModel(
    private val substrateApplication: SubstrateLedgerApplication,
    private val discoveryService: LedgerDeviceDiscoveryService,
    private val permissionsAsker: PermissionsAsker.Presentation,
    private val bluetoothManager: BluetoothManager,
    private val router: ReturnableRouter,
) : BaseViewModel() {

    private val stateMachine = StateMachine(WaitingForPermissionsState(), coroutineScope = this)

//    val canTrackBluetooth = stateMachine.state.map { it !is WaitingForPermissionsState }
//        .distinctUntilChanged()
//        .shareInBackground()

    val deviceModels = stateMachine.state.map(::mapStateToUi)
        .shareInBackground()

    init {
        requirePermissions()

        handleSideEffects()

        emitInitialBluetoothState()
    }

    private fun emitInitialBluetoothState() {
        val state = if (bluetoothManager.isBluetoothEnabled()) {
            BluetoothState.ON
        } else {
            BluetoothState.OFF
        }

        bluetoothStateChanged(state)
    }

    abstract fun connectedToDevice(device: LedgerDevice, account: LedgerSubstrateAccount)

    fun deviceClicked(item: SelectLedgerModel) = launch {
        discoveryService.findDevice(item.id)?.let { device ->
            stateMachine.onEvent(Event.DeviceChosen(device))
        }
    }

    fun bluetoothStateChanged(state: BluetoothState) {
        when (state) {
            BluetoothState.ON -> stateMachine.onEvent(Event.BluetoothEnabled)
            BluetoothState.OFF -> stateMachine.onEvent(Event.BluetoothDisabled)
        }
    }

    fun backClicked() {
        router.back()
    }

    private fun handleSideEffects() {
        launch {
            for (effect in stateMachine.sideEffects) {
                handleSideEffect(effect)
            }
        }
    }

    private fun handleSideEffect(effect: SideEffect) {
        when(effect) {
            is SideEffect.ConnectToDevice -> connectToDevice(effect.device)

            SideEffect.EnableBluetooth -> bluetoothManager.enableBluetooth()

            is SideEffect.PresentConnectionFailure -> showError(effect.reason.message ?: "Error")

            is SideEffect.StartDeviceFlow -> connectedToDevice(effect.device, effect.checkedAccount)

            SideEffect.StartDiscovery -> {
                discoveryService.performDiscovery(scope = this)
                discoveryService.errors.onEach(::discoveryError)

                discoveryService.discoveredDevices.onEach {
                    stateMachine.onEvent(Event.DiscoveredDevicesListChanged(it))
                }.launchIn(this)
            }
        }
    }

    private fun connectToDevice(device: LedgerDevice) = launch {
        device.connection.connect().mapCatching {
            substrateApplication.getAccount(device, Chain.Geneses.POLKADOT, accountIndex = 0)
        }
            .onSuccess {
                stateMachine.onEvent(Event.ConnectionSucceeded(it))
            }.onFailure {
                stateMachine.onEvent(Event.ConnectionFailed(it))
            }
    }

    private fun requirePermissions() = launch {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val granted = permissionsAsker.requirePermissionsOrExit(*permissions.toTypedArray())

        if (granted) {
            stateMachine.onEvent(Event.PermissionsGranted)
        }
    }

    private fun discoveryError(error: Throwable) {
        when(error) {
            is BleScanFailed -> stateMachine.onEvent(Event.BluetoothDisabled)
        }
    }

    private fun mapStateToUi(state: SelectLedgerState): List<SelectLedgerModel> {
        return when(state) {
            is DeviceFlowStarted -> mapDevicesToUi(state.devices, connectingTo = null)
            is DevicesFoundState -> mapDevicesToUi(state.devices, connectingTo = state.connectingDevice)
            is DiscoveringState -> emptyList()
            is NoBluetoothState -> emptyList()
            is WaitingForPermissionsState -> emptyList()
        }
    }

    private fun mapDevicesToUi(devices: List<LedgerDevice>, connectingTo: LedgerDevice?): List<SelectLedgerModel> {
        return devices.map {
            SelectLedgerModel(
                id = it.id,
                name = it.name,
                isConnecting = it.id == connectingTo?.id
            )
        }
    }
}
