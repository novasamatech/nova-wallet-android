package io.novafoundation.nova.feature_account_impl.presentation.pincode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.sequrity.BackgroundAccessObserver
import io.novafoundation.nova.common.vibration.DeviceVibrator
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import kotlinx.coroutines.launch

class PinCodeViewModel(
    private val interactor: AccountInteractor,
    private val router: AccountRouter,
    private val deviceVibrator: DeviceVibrator,
    private val resourceManager: ResourceManager,
    private val backgroundAccessObserver: BackgroundAccessObserver,
    val pinCodeAction: PinCodeAction
) : BaseViewModel() {

    sealed class ScreenState {
        object Creating : ScreenState()
        data class Confirmation(val codeToConfirm: String) : ScreenState()
        object Checking : ScreenState()
    }

    private val _homeButtonVisibilityLiveData = MutableLiveData(pinCodeAction.toolbarConfiguration.backVisible)
    val homeButtonVisibilityLiveData: LiveData<Boolean> = _homeButtonVisibilityLiveData

    private val _resetInputEvent = MutableLiveData<Event<String>>()
    val resetInputEvent: LiveData<Event<String>> = _resetInputEvent

    private val _matchingPincodeErrorEvent = MutableLiveData<Event<Unit>>()
    val matchingPincodeErrorEvent: LiveData<Event<Unit>> = _matchingPincodeErrorEvent

    private val _showFingerPrintEvent = MutableLiveData<Event<Unit>>()
    val showFingerPrintEvent: LiveData<Event<Unit>> = _showFingerPrintEvent

    private val _startFingerprintScannerEventLiveData = MutableLiveData<Event<Unit>>()
    val startFingerprintScannerEventLiveData: LiveData<Event<Unit>> = _startFingerprintScannerEventLiveData

    private val _fingerPrintErrorEvent = MutableLiveData<Event<String>>()
    val fingerPrintErrorEvent: LiveData<Event<String>> = _fingerPrintErrorEvent

    private val _biometricSwitchDialogLiveData = MutableLiveData<Event<Unit>>()
    val biometricSwitchDialogLiveData: LiveData<Event<Unit>> = _biometricSwitchDialogLiveData

    private var fingerPrintAvailable = false
    private var currentState: ScreenState? = null

    val isBackRoutingBlocked: Boolean
        get() = pinCodeAction is PinCodeAction.RecoveryAuthorisation

    fun startAuth() {
        when (pinCodeAction) {
            is PinCodeAction.Create -> {
                currentState = ScreenState.Creating
            }
            is PinCodeAction.RecoveryAuthorisation,
            is PinCodeAction.Check -> {
                currentState = ScreenState.Checking
                _showFingerPrintEvent.value = Event(Unit)
            }
            is PinCodeAction.Change -> {
                currentState = ScreenState.Checking
                _showFingerPrintEvent.value = Event(Unit)
            }
        }
    }

    fun pinCodeEntered(pin: String) {
        when (currentState) {
            is ScreenState.Creating -> tempCodeEntered(pin)
            is ScreenState.Confirmation -> matchPinCodeWithCodeToConfirm(pin, (currentState as ScreenState.Confirmation).codeToConfirm)
            is ScreenState.Checking -> checkPinCode(pin)
            null -> {}
        }
    }

    private fun tempCodeEntered(pin: String) {
        _resetInputEvent.value = Event(resourceManager.getString(R.string.pincode_confirm_your_pin_code))
        _homeButtonVisibilityLiveData.value = true
        currentState = ScreenState.Confirmation(pin)
    }

    private fun matchPinCodeWithCodeToConfirm(pinCode: String, codeToConfirm: String) {
        if (codeToConfirm == pinCode) {
            registerPinCode(pinCode)
        } else {
            deviceVibrator.makeShortVibration()
            _matchingPincodeErrorEvent.value = Event(Unit)
        }
    }

    private fun registerPinCode(code: String) {
        viewModelScope.launch {
            interactor.savePin(code)

            if (fingerPrintAvailable && pinCodeAction is PinCodeAction.Create) {
                _biometricSwitchDialogLiveData.value = Event(Unit)
            } else {
                authSuccess()
            }
        }
    }

    private fun checkPinCode(code: String) {
        viewModelScope.launch {
            val isCorrect = interactor.isPinCorrect(code)

            if (isCorrect) {
                authSuccess()
            } else {
                deviceVibrator.makeShortVibration()
                _matchingPincodeErrorEvent.value = Event(Unit)
            }
        }
    }

    fun backPressed() {
        when (currentState) {
            is ScreenState.Creating -> authCancel()
            is ScreenState.Confirmation -> backToCreateFromConfirmation()
            is ScreenState.Checking -> authCancel()
            null -> {}
        }
    }

    private fun backToCreateFromConfirmation() {
        _resetInputEvent.value = Event(resourceManager.getString(R.string.pincode_enter_pin_code_v2_2_0))

        if (pinCodeAction is PinCodeAction.Create) {
            _homeButtonVisibilityLiveData.value = pinCodeAction.toolbarConfiguration.backVisible
        }

        currentState = ScreenState.Creating
    }

    fun onResume() {
        viewModelScope.launch {
            if (ScreenState.Checking == currentState && interactor.isBiometricEnabled()) {
                _startFingerprintScannerEventLiveData.value = Event(Unit)
            }
        }
    }

    fun onAuthenticationError(errString: String) {
        _fingerPrintErrorEvent.value = Event(errString)
    }

    fun biometryAuthenticationSucceeded() {
        authSuccess()
    }

    fun biometryAuthenticationFailed() {
        _fingerPrintErrorEvent.value = Event(resourceManager.getString(R.string.pincode_fingerprint_error))
    }

    fun fingerprintScannerAvailable(authReady: Boolean) {
        fingerPrintAvailable = authReady
    }

    private fun authSuccess() {
        when (pinCodeAction) {
            is PinCodeAction.Create -> router.openAfterPinCode(pinCodeAction.delayedNavigation)
            is PinCodeAction.Check -> {
                router.openAfterPinCode(pinCodeAction.delayedNavigation)
            }
            is PinCodeAction.RecoveryAuthorisation -> {
                backgroundAccessObserver.onAccessed()
                router.openAfterPinCode(pinCodeAction.delayedNavigation)
            }
            is PinCodeAction.Change -> {
                when (currentState) {
                    is ScreenState.Checking -> {
                        currentState = ScreenState.Creating
                        _resetInputEvent.value = Event(resourceManager.getString(R.string.pincode_create_top_title))
                        _homeButtonVisibilityLiveData.value = true
                    }
                    is ScreenState.Confirmation -> {
                        router.back()
                        showMessage(resourceManager.getString(R.string.pincode_changed_message))
                    }
                    else -> {}
                }
            }
        }
    }

    private fun authCancel() {
        router.back()
    }

    fun acceptAuthWithBiometry() {
        viewModelScope.launch {
            interactor.setBiometricOn()

            authSuccess()
        }
    }

    fun declineAuthWithBiometry() {
        viewModelScope.launch {
            interactor.setBiometricOff()

            authSuccess()
        }
    }

    fun finishApp() {
        router.finishApp()
    }
}
