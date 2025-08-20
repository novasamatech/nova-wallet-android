package io.novafoundation.nova.feature_push_notifications.presentation.multisigsWarning

import io.novafoundation.nova.common.base.BaseScreenMixin


fun BaseScreenMixin<*>.observeEnableMultisigPushesAlert(mixin: MultisigPushNotificationsAlertMixin) {
    mixin.showWarningEvent.observeEvent {
        MultisigPushNotificationsAlertBottomSheet(providedContext, onEnableClicked = { mixin.showPushSettings() }).show()
    }
}
