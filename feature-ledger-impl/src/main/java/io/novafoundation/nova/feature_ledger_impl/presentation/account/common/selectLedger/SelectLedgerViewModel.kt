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
import io.novafoundation.nova.common.utils.permissions.checkPermissions
import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryMethods
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryRequirement
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryRequirementAvailability
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.discoveryRequirements
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.findDevice
import io.novafoundation.nova.feature_ledger_impl.R
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommand
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.LedgerMessageCommands
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.MessageCommandFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.bottomSheet.mappers.LedgerDeviceFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.errors.handleLedgerError
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters.LedgerMessageFormatter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.model.SelectLedgerModel
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SelectLedgerEvent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.SideEffect
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states.DevicesFoundState
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states.DiscoveringState
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine.states.SelectLedgerState
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
    private val payload: SelectLedgerPayload,
    private val ledgerDeviceFormatter: LedgerDeviceFormatter,
    private val messageCommandFormatter: MessageCommandFormatter,
) : BaseViewModel(),
    PermissionsAsker by permissionsAsker,
    LedgerMessageCommands,
    Browserable.Presentation by Browserable() {

    private val discoveryMethods = payload.connectionMode.toDiscoveryMethod()

    private val stateMachine = StateMachine(createInitialState(), coroutineScope = this)

    val deviceModels = stateMachine.state.map(::mapStateToUi)
        .shareInBackground()

    val hints = flowOf {
        when (payload.connectionMode) {
            SelectLedgerPayload.ConnectionMode.BLUETOOTH -> resourceManager.getString(
                R.string.account_ledger_select_device_description,
                messageFormatter.appName()
            )

            SelectLedgerPayload.ConnectionMode.USB -> resourceManager.getString(
                R.string.account_ledger_select_device_usb_description,
                messageFormatter.appName()
            )

            SelectLedgerPayload.ConnectionMode.ALL -> resourceManager.getString(R.string.account_ledger_select_device_all_description)
        }
    }.shareInBackground()

    val showPermissionsButton = flowOf { payload.connectionMode == SelectLedgerPayload.ConnectionMode.ALL }
        .shareInBackground()

    override val ledgerMessageCommands = MutableLiveData<Event<LedgerMessageCommand>>()

    private val _showRequestLocationDialog = MutableLiveData<Boolean>()
    val showRequestLocationDialog: LiveData<Boolean> = _showRequestLocationDialog

    init {
        handleSideEffects()
        setupDiscoveryObserving()
    }

    abstract suspend fun verifyConnection(device: LedgerDevice)

    open suspend fun handleLedgerError(reason: Throwable, device: LedgerDevice) {
        handleLedgerError(
            reason = reason,
            device = device,
            commandFormatter = messageCommandFormatter,
            onRetry = { stateMachine.onEvent(SelectLedgerEvent.DeviceChosen(device)) }
        )
    }

    open fun backClicked() {
        router.back()
    }

    fun allowAvailabilityRequests() {
        stateMachine.onEvent(SelectLedgerEvent.AvailabilityRequestsAllowed)
    }

    fun deviceClicked(item: SelectLedgerModel) = launch {
        discoveryService.findDevice(item.id)?.let { device ->
            stateMachine.onEvent(SelectLedgerEvent.DeviceChosen(device))
        }
    }

    fun bluetoothStateChanged(state: BluetoothState) {
        when (state) {
            BluetoothState.ON -> stateMachine.onEvent(SelectLedgerEvent.DiscoveryRequirementSatisfied(DiscoveryRequirement.BLUETOOTH))
            BluetoothState.OFF -> stateMachine.onEvent(SelectLedgerEvent.DiscoveryRequirementMissing(DiscoveryRequirement.BLUETOOTH))
        }
    }

    fun locationStateChanged() {
        emitLocationState()
    }

    fun enableLocationAcknowledged() {
        locationManager.enableLocation()
    }

    override fun onCleared() {
        discoveryService.stopDiscovery()
    }

    private fun emitLocationState() {
        when (locationManager.isLocationEnabled()) {
            true -> stateMachine.onEvent(SelectLedgerEvent.DiscoveryRequirementSatisfied(DiscoveryRequirement.LOCATION))
            false -> stateMachine.onEvent(SelectLedgerEvent.DiscoveryRequirementMissing(DiscoveryRequirement.LOCATION))
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

    private suspend fun handleSideEffect(effect: SideEffect) {
        when (effect) {
            is SideEffect.PresentLedgerFailure -> launch { handleLedgerError(effect.reason, effect.device) }

            is SideEffect.VerifyConnection -> performConnectionVerification(effect.device)

            is SideEffect.StartDiscovery -> discoveryService.startDiscovery(effect.methods)

            is SideEffect.RequestPermissions -> requestPermissions(effect.requirements, effect.shouldExitUponDenial)

            is SideEffect.RequestSatisfyRequirement -> requestSatisfyRequirement(effect.requirements)

            is SideEffect.StopDiscovery -> discoveryService.stopDiscovery(effect.methods)
        }
    }

    private suspend fun requestSatisfyRequirement(requirements: List<DiscoveryRequirement>) {
        val (awaitable, fireAndForget) = requirements.map { it.createRequest() }
            .partition { it.awaitable }

        // Do awaitable requests first to reduce the change of overlapping requests happening
        // With this logic overlapping requests may only happen if there are more than one `fireAndForget` requirement
        // Important: permissions are handled separately and state machine ensures permissions are always requested first
        awaitable.forEach { it.requestAction() }
        fireAndForget.forEach { it.requestAction() }
    }

    private suspend fun requestPermissions(
        discoveryRequirements: List<DiscoveryRequirement>,
        shouldExitUponDenial: Boolean
    ): Boolean {
        val permissions = discoveryRequirements.requiredPermissions()

        val granted = permissionsAsker.requirePermissions(*permissions.toTypedArray())

        if (granted) {
            stateMachine.onEvent(SelectLedgerEvent.PermissionsGranted)
        } else {
            onPermissionsNotGranted(shouldExitUponDenial)
        }

        return granted
    }

    private fun setupDiscoveryObserving() {
        discoveryService.errors.onEach(::discoveryError)

        discoveryService.discoveredDevices.onEach {
            stateMachine.onEvent(SelectLedgerEvent.DiscoveredDevicesListChanged(it))
        }.launchIn(this)
    }

    private fun onPermissionsNotGranted(shouldExitUponDenial: Boolean) {
        if (shouldExitUponDenial) {
            router.back()
        }
    }

    private fun discoveryError(error: Throwable) {
        when (error) {
            is BleScanFailed -> {
                val event = SelectLedgerEvent.DiscoveryRequirementMissing(DiscoveryRequirement.BLUETOOTH)
                stateMachine.onEvent(event)
            }
        }
    }

    private fun mapStateToUi(state: SelectLedgerState): List<SelectLedgerModel> {
        return when (state) {
            is DevicesFoundState -> mapDevicesToUi(state.devices, connectingTo = state.verifyingDevice)
            is DiscoveringState -> emptyList()
        }
    }

    private fun mapDevicesToUi(devices: List<LedgerDevice>, connectingTo: LedgerDevice?): List<SelectLedgerModel> {
        return devices.map {
            SelectLedgerModel(
                id = it.id,
                name = ledgerDeviceFormatter.formatName(it),
                isConnecting = it.id == connectingTo?.id
            )
        }
    }

    private fun SelectLedgerPayload.ConnectionMode.toDiscoveryMethod(): DiscoveryMethods {
        return when (this) {
            SelectLedgerPayload.ConnectionMode.BLUETOOTH -> DiscoveryMethods(DiscoveryMethods.Method.BLE)
            SelectLedgerPayload.ConnectionMode.USB -> DiscoveryMethods(DiscoveryMethods.Method.USB)
            SelectLedgerPayload.ConnectionMode.ALL -> DiscoveryMethods(DiscoveryMethods.Method.BLE, DiscoveryMethods.Method.USB)
        }
    }

    private fun List<DiscoveryRequirement>.requiredPermissions() = flatMap { it.requiredPermissions() }

    private fun DiscoveryRequirement.requiredPermissions() = when (this) {
        DiscoveryRequirement.BLUETOOTH -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                listOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            } else {
                emptyList()
            }
        }

        DiscoveryRequirement.LOCATION -> listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun createInitialState(): SelectLedgerState {
        val allRequirements = discoveryMethods.discoveryRequirements()
        val permissionsGranted = permissionsAsker.checkPermissions(allRequirements.requiredPermissions())

        val satisfiedDiscoverRequirements = setOfNotNull(
            DiscoveryRequirement.BLUETOOTH.takeIf { bluetoothManager.isBluetoothEnabled() },
            DiscoveryRequirement.LOCATION.takeIf { locationManager.isLocationEnabled() }
        )

        val availability = DiscoveryRequirementAvailability(satisfiedDiscoverRequirements, permissionsGranted)

        return DiscoveringState.initial(discoveryMethods, availability)
    }

    private fun DiscoveryRequirement.createRequest(): DiscoveryRequirementRequest {
        return when (this) {
            DiscoveryRequirement.BLUETOOTH -> DiscoveryRequirementRequest.awaitable { bluetoothManager.enableBluetoothAndAwait() }
            DiscoveryRequirement.LOCATION -> DiscoveryRequirementRequest.fireAndForget { requestLocation() }
        }
    }

    private fun requestLocation() {
        _showRequestLocationDialog.value = true
    }

    private class DiscoveryRequirementRequest(
        val requestAction: suspend () -> Unit,
        val awaitable: Boolean
    ) {

        companion object {

            fun awaitable(requestAction: suspend () -> Unit): DiscoveryRequirementRequest {
                return DiscoveryRequirementRequest(requestAction, awaitable = true)
            }

            fun fireAndForget(requestAction: () -> Unit): DiscoveryRequirementRequest {
                return DiscoveryRequirementRequest(requestAction, awaitable = false)
            }
        }
    }
}
