package io.novafoundation.nova.feature_push_notifications.presentation.welcome

import android.Manifest
import android.os.Build
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.domain.interactor.PushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.WelcomePushNotificationsInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PushWelcomeViewModel(
    private val router: PushNotificationsRouter,
    private val pushNotificationsInteractor: PushNotificationsInteractor,
    private val welcomePushNotificationsInteractor: WelcomePushNotificationsInteractor,
    private val permissionsAsker: PermissionsAsker.Presentation,
    private val resourceManager: ResourceManager
) : BaseViewModel(), PermissionsAsker by permissionsAsker, Retriable {

    private val _enablingInProgress = MutableStateFlow(false)

    override val retryEvent: MutableLiveData<Event<RetryPayload>> = MutableLiveData()

    val buttonState = _enablingInProgress.map { inProgress ->
        when (inProgress) {
            true -> ButtonState.PROGRESS
            false -> ButtonState.NORMAL
        }
    }

    fun backClicked() {
        welcomePushNotificationsInteractor.setWelcomeScreenShown()
        router.back()
    }

    fun termsClicked() {
        // Need to implement
    }

    fun privacyClicked() {
        // Need to implement
    }

    fun askPermissionAndOpenSettings() {
        launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val isPermissionsGranted = permissionsAsker.requirePermissionsOrExit(Manifest.permission.POST_NOTIFICATIONS)

                if (!isPermissionsGranted) {
                    return@launch
                }
            }

            _enablingInProgress.value = true
            pushNotificationsInteractor.initPushSettings()
                .onSuccess {
                    welcomePushNotificationsInteractor.setWelcomeScreenShown()
                    router.openPushSettings()
                }
                .onFailure { retryDialog() }

            _enablingInProgress.value = false
        }
    }

    private fun retryDialog() {
        retryEvent.value = Event(
            RetryPayload(
                title = resourceManager.getString(R.string.common_error_general_title),
                message = resourceManager.getString(R.string.common_retry_message),
                onRetry = { askPermissionAndOpenSettings() }
            )
        )
    }
}
