package io.novafoundation.nova.feature_push_notifications.data.di

import io.novafoundation.nova.feature_push_notifications.data.NovaFirebaseMessagingService
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.PushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.WelcomePushNotificationsInteractor

interface PushNotificationsFeatureApi {

    fun inject(service: NovaFirebaseMessagingService)

    fun pushNotificationInteractor(): PushNotificationsInteractor

    fun welcomePushNotificationsInteractor(): WelcomePushNotificationsInteractor
}
