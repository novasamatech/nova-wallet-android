package io.novafoundation.nova.app.root.presentation.main

import io.novafoundation.nova.app.root.presentation.RootRouter
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_push_notifications.domain.interactor.WelcomePushNotificationsInteractor
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor

class MainViewModel(
    updateNotificationsInteractor: UpdateNotificationsInteractor,
    private val automaticInteractionGate: AutomaticInteractionGate,
    private val welcomePushNotificationsInteractor: WelcomePushNotificationsInteractor,
    private val rootRouter: RootRouter
) : BaseViewModel() {

    init {
        updateNotificationsInteractor.allowInAppUpdateCheck()
        automaticInteractionGate.initialPinPassed()

        if (welcomePushNotificationsInteractor.needToShowWelcomeScreen()) {
            rootRouter.openPushWelcome()
        }
    }
}
