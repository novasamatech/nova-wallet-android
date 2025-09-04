package io.novafoundation.nova.feature_push_notifications.di

import io.novafoundation.nova.feature_push_notifications.NovaFirebaseMessagingService
import io.novafoundation.nova.feature_push_notifications.domain.interactor.PushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.WelcomePushNotificationsInteractor

interface PushNotificationsFeatureApi {

    fun inject(service: NovaFirebaseMessagingService)

    fun pushNotificationInteractor(): PushNotificationsInteractor

    fun welcomePushNotificationsInteractor(): WelcomePushNotificationsInteractor
}
