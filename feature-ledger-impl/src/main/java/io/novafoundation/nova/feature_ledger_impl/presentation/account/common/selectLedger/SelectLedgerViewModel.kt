package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger

import android.Manifest
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.location.LocationManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.findDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.performDiscovery
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommands
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.errors.handleLedgerError
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.model.SelectLedgerModel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states.DevicesFoundState
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states.DiscoveringState
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states.MissingDiscoveryRequirementState
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states.SelectLedgerState
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states.WaitingForPermissionsState
import io.novafoundation.nova.feature_ledger_impl.sdk.discovery.ble.BleScanFailed
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

enum class BluetoothState {
    ON, OFF
}

abstract class SelectLedgerViewModel(
    private val discoveryService: LedgerDeviceDiscoveryService,
    private val permissionsAsker: PermissionsAsker.Presentation,
    private val bluetoothManager: BluetoothManager,
    private val locationManager: LocationManager,
    private val router: ReturnableRouter,
    private val resourceManager: ResourceManager,
    private val messageFormatter: LedgerMessageFormatter,
) : BaseViewModel(),
    PermissionsAsker by permissionsAsker,
    LedgerMessageCommands,
    Browserable.Presentation by Browserable() {

    private val stateMachine = StateMachine(WaitingForPermissionsState(), coroutineScope = this)

    val deviceModels = stateMachine.state.map(::mapStateToUi)
        .shareInBackground()

    val hints = flowOf {
        resourceManager.getString(R.string.account_ledger_select_device_description, messageFormatter.appName())
    }.shareInBackground()

    override val ledgerMessageCommands = MutableLiveData<Event<LedgerMessageCommand>>()

    private val _showRequestLocationDialog = MutableLiveData<Boolean>()
    val showRequestLocationDialog: LiveData<Boolean> = _showRequestLocationDialog

    private var isLocationEnabled: Boolean? = null

    init {
        emitInitialBluetoothState()
        emitLocationState()

        requirePermissions()

        handleSideEffects()
    }

    abstract suspend fun verifyConnection(device: LedgerDevice)

    open suspend fun handleLedgerError(reason: Throwable, device: LedgerDevice) {
        handleLedgerError(
            reason = reason,
            messageFormatter = messageFormatter,
            resourceManager = resourceManager,
            retry = { stateMachine.onEvent(SelectLedgerEvent.DeviceChosen(device)) }
        )
    }

    open fun backClicked() {
        router.back()
    }

    fun deviceClicked(item: SelectLedgerModel) = launch {
        discoveryService.findDevice(item.id)?.let { device ->
            stateMachine.onEvent(SelectLedgerEvent.DeviceChosen(device))
        }
    }

    fun bluetoothStateChanged(state: BluetoothState) {
        when (state) {
            BluetoothState.ON -> stateMachine.onEvent(SelectLedgerEvent.BluetoothEnabled)
            BluetoothState.OFF -> stateMachine.onEvent(SelectLedgerEvent.BluetoothDisabled)
        }
    }

    fun locationStateChanged() {
        val newLocationState = locationManager.isLocationEnabled()
        if (isLocationEnabled == newLocationState) return
        isLocationEnabled = newLocationState

        emitLocationState()
    }

    fun enableLocation() {
        locationManager.enableLocation()
    }

    private fun emitInitialBluetoothState() {
        val state = if (bluetoothManager.isBluetoothEnabled()) {
            BluetoothState.ON
        } else {
            BluetoothState.OFF
        }

        bluetoothStateChanged(state)
    }

    private fun emitLocationState() {
        when (locationManager.isLocationEnabled()) {
            true -> stateMachine.onEvent(SelectLedgerEvent.LocationEnabled)
            false -> stateMachine.onEvent(SelectLedgerEvent.LocationDisabled)
        }
    }

    private fun handleSideEffects() {
        launch {
            for (effect in stateMachine.sideEffects) {
                handleSideEffect(effect)
            }
        }
    }

    private fun performConnectionVerification(device: LedgerDevice) = launch {
        runCatching { verifyConnection(device) }
            .onSuccess { stateMachine.onEvent(SelectLedgerEvent.ConnectionVerified) }
            .onFailure { stateMachine.onEvent(SelectLedgerEvent.VerificationFailed(it)) }
    }

    private fun handleSideEffect(effect: SideEffect) {
        when (effect) {
            SideEffect.EnableBluetooth -> bluetoothManager.enableBluetooth()

            SideEffect.EnableLocation -> {
                requestLocation()
            }

            is SideEffect.PresentLedgerFailure -> launch { handleLedgerError(effect.reason, effect.device) }

            is SideEffect.VerifyConnection -> performConnectionVerification(effect.device)

            SideEffect.StartDiscovery -> startDeviceDiscovery()
        }
    }

    private fun requestLocation() {
        _showRequestLocationDialog.value = true
    }

    private fun startDeviceDiscovery() {
        discoveryService.performDiscovery(scope = this)
        discoveryService.errors.onEach(::discoveryError)

        discoveryService.discoveredDevices.onEach {
            stateMachine.onEvent(SelectLedgerEvent.DiscoveredDevicesListChanged(it))
        }.launchIn(this)
    }

    private fun requirePermissions() = launch {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val granted = permissionsAsker.requirePermissionsOrExit(*permissions.toTypedArray())

        if (granted) {
            stateMachine.onEvent(SelectLedgerEvent.PermissionsGranted)
        }
    }

    private fun discoveryError(error: Throwable) {
        when (error) {
            is BleScanFailed -> stateMachine.onEvent(SelectLedgerEvent.BluetoothDisabled)
        }
    }

    private fun mapStateToUi(state: SelectLedgerState): List<SelectLedgerModel> {
        return when (state) {
            is DevicesFoundState -> mapDevicesToUi(state.devices, connectingTo = state.verifyingDevice)
            is DiscoveringState -> emptyList()
            is MissingDiscoveryRequirementState -> emptyList()
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
