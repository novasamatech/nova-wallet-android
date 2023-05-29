package io.novafoundation.nova.feature_account_impl.presentation.pincode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationDialogInfo
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingOrDenyingAction
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.sequrity.BiometricResponse
import io.novafoundation.nova.common.sequrity.BiometricService
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationExecutor
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.sequrity.BackgroundAccessObserver
import io.novafoundation.nova.common.vibration.DeviceVibrator
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.common.sequrity.biometry.mapBiometricErrors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class PinCodeViewModel(
    private val interactor: AccountInteractor,
    private val router: AccountRouter,
    private val deviceVibrator: DeviceVibrator,
    private val resourceManager: ResourceManager,
    private val backgroundAccessObserver: BackgroundAccessObserver,
    private val twoFactorVerificationExecutor: TwoFactorVerificationExecutor,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val biometricService: BiometricService,
    val pinCodeAction: PinCodeAction
) : BaseViewModel() {

    sealed class ScreenState {
        object Creating : ScreenState()
        data class Confirmation(val codeToConfirm: String) : ScreenState()
        data class Checking(val useBiometry: Boolean) : ScreenState()
    }

    val confirmationAwaitableAction = actionAwaitableMixinFactory.confirmingOrDenyingAction<ConfirmationDialogInfo>()

    private val _homeButtonVisibilityLiveData = MutableLiveData(pinCodeAction.toolbarConfiguration.backVisible)
    val homeButtonVisibilityLiveData: LiveData<Boolean> = _homeButtonVisibilityLiveData

    private val _resetInputEvent = MutableLiveData<Event<String>>()
    val resetInputEvent: LiveData<Event<String>> = _resetInputEvent

    private val _matchingPincodeErrorEvent = MutableLiveData<Event<Unit>>()
    val matchingPincodeErrorEvent: LiveData<Event<Unit>> = _matchingPincodeErrorEvent

    private val _showBiometryEvent = MutableLiveData<Event<Boolean>>()
    val showFingerPrintEvent: LiveData<Event<Boolean>> = _showBiometryEvent

    val biometricEvents = biometricService.biometryServiceResponseFlow
        .mapNotNull { mapBiometricErrors(resourceManager, it) }
        .shareInBackground()

    private var currentState: ScreenState? = null

    val isBackRoutingBlocked: Boolean
        get() = pinCodeAction is PinCodeAction.CheckAfterInactivity

    init {
        handleBiometryServiceEvents()
    }

    fun startAuth() {
        when (pinCodeAction) {
            is PinCodeAction.Create -> {
                currentState = ScreenState.Creating
            }
            is PinCodeAction.Check,
            is PinCodeAction.Change -> {
                currentState = ScreenState.Checking(true)
                _showBiometryEvent.value = Event(biometricService.isBiometricReady() && biometricService.isEnabled())
            }
            is PinCodeAction.TwoFactorVerification -> {
                currentState = ScreenState.Checking(pinCodeAction.useBiometryIfEnabled)
                if (pinCodeAction.useBiometryIfEnabled) {
                    _showBiometryEvent.value = Event(biometricService.isBiometricReady() && biometricService.isEnabled())
                }
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

            if (biometricService.isBiometricReady() && pinCodeAction is PinCodeAction.Create) {
                askForBiometry()
            }

            authSuccess()
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
            is ScreenState.Confirmation -> backToCreateFromConfirmation()
            is ScreenState.Creating,
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
        if (currentState is ScreenState.Checking &&
            (currentState as ScreenState.Checking).useBiometry &&
            biometricService.isEnabled()
        ) {
            startBiometryAuth()
        }
    }

    fun onPause() {
        biometricService.cancel()
    }

    private fun authSuccess() {
        when (pinCodeAction) {
            is PinCodeAction.Create -> router.openAfterPinCode(pinCodeAction.delayedNavigation)
            is PinCodeAction.Check -> {
                router.openAfterPinCode(pinCodeAction.delayedNavigation)
                backgroundAccessObserver.checkPassed()
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
            is PinCodeAction.TwoFactorVerification -> {
                twoFactorVerificationExecutor.confirm()
                router.back()
            }
        }
    }

    fun startBiometryAuth() {
        biometricService.requestBiometric()
    }

    private fun handleBiometryServiceEvents() {
        biometricService.biometryServiceResponseFlow
            .filterIsInstance<BiometricResponse.Success>()
            .onEach { authSuccess() }
            .launchIn(this)
    }

    private suspend fun askForBiometry() {
        val isSuccess = try {
            confirmationAwaitableAction.awaitAction(
                ConfirmationDialogInfo(
                    title = R.string.pincode_biometry_dialog_title,
                    message = R.string.pincode_biometric_switch_dialog_title,
                    positiveButton = R.string.common_use,
                    negativeButton = R.string.common_skip
                )
            )
        } catch (e: CancellationException) {
            false
        }

        biometricService.enableBiometry(isSuccess)
    }

    private fun authCancel() {
        if (pinCodeAction is PinCodeAction.TwoFactorVerification) {
            twoFactorVerificationExecutor.cancel()
        }
        router.back()
    }

    fun finishApp() {
        router.finishApp()
    }
}
