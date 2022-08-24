package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger

import android.Manifest
import android.os.Build
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerApplicationResponse
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerApplicationResponse.appNotOpen
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerApplicationResponse.transactionRejected
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerApplicationResponse.wrongAppOpen
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplication
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateLedgerApplicationError
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.findDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.performDiscovery
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.model.SelectLedgerModel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states.DeviceFlowStarted
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states.DevicesFoundState
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states.DiscoveringState
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states.NoBluetoothState
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states.SelectLedgerState
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states.WaitingForPermissionsState
import io.novafoundation.nova.feature_ledger_impl.sdk.discovery.ble.BleScanFailed
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
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
    private val resourceManager: ResourceManager,
    private val payload: SelectLedgerPayload,
    private val chainRegistry: ChainRegistry,
) : BaseViewModel(), PermissionsAsker by permissionsAsker, Retriable {

    private val chain by lazyAsync { chainRegistry.getChain(payload.chainId) }

    override val retryEvent = MutableLiveData<Event<RetryPayload>>()

    private val stateMachine = StateMachine(WaitingForPermissionsState(), coroutineScope = this)

    val deviceModels = stateMachine.state.map(::mapStateToUi)
        .shareInBackground()

    init {
        requirePermissions()

        handleSideEffects()

        emitInitialBluetoothState()
    }

    abstract suspend fun verifyConnection(device: LedgerDevice)

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

    fun backClicked() {
        router.back()
    }


    private fun emitInitialBluetoothState() {
        val state = if (bluetoothManager.isBluetoothEnabled()) {
            BluetoothState.ON
        } else {
            BluetoothState.OFF
        }

        bluetoothStateChanged(state)
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
            .onFailure { stateMachine.onEvent(SelectLedgerEvent.ConnectionFailed(it)) }
    }

    private fun handleSideEffect(effect: SideEffect) {
        when (effect) {
            is SideEffect.ConnectToDevice -> connectToDevice(effect.device)

            SideEffect.EnableBluetooth -> bluetoothManager.enableBluetooth()

            is SideEffect.PresentConnectionFailure -> {
                effect.reason.printStackTrace()

                handleConnectionFailure(effect.device, effect.reason)
            }

            is SideEffect.VerifyConnection -> performConnectionVerification(effect.device)

            SideEffect.StartDiscovery -> {
                discoveryService.performDiscovery(scope = this)
                discoveryService.errors.onEach(::discoveryError)

                discoveryService.discoveredDevices.onEach {
                    stateMachine.onEvent(SelectLedgerEvent.DiscoveredDevicesListChanged(it))
                }.launchIn(this)
            }
        }
    }

    private fun handleConnectionFailure(device: LedgerDevice, reason: Throwable) = launch {
        when (reason) {
            is SubstrateLedgerApplicationError.Response -> handleSubstrateApplicationError(device, reason.response)
            else -> showError(
                title = resourceManager.getString(R.string.ledger_error_general_title),
                text = resourceManager.getString(R.string.ledger_error_general_message)
            )
        }
    }

    private suspend fun handleSubstrateApplicationError(device: LedgerDevice, reason: LedgerApplicationResponse) {
        val errorTitle: String
        val errorMessage: String

        when (reason) {
            appNotOpen, wrongAppOpen -> {
                errorTitle = resourceManager.getString(R.string.ledger_error_app_not_launched_title, chain().name)
                errorMessage = resourceManager.getString(R.string.ledger_error_app_not_launched_message, chain().name)
            }

            transactionRejected -> {
                errorTitle = resourceManager.getString(R.string.ledger_error_app_cancelled_title)
                errorMessage = resourceManager.getString(R.string.ledger_error_app_cancelled_message)
            }

            else -> {
                errorTitle = resourceManager.getString(R.string.ledger_error_general_title)
                errorMessage = resourceManager.getString(R.string.ledger_error_general_message)
            }
        }

        retryEvent.value = RetryPayload(
            title = errorTitle,
            message = errorMessage,
            onRetry = { stateMachine.onEvent(SelectLedgerEvent.DeviceChosen(device)) }
        ).event()
    }

    private fun connectToDevice(device: LedgerDevice) = launch {
        device.connection.connect().onSuccess {
            stateMachine.onEvent(SelectLedgerEvent.ConnectionSucceeded)
        }.onFailure {
            stateMachine.onEvent(SelectLedgerEvent.ConnectionFailed(it))
        }
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
