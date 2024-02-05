package io.novafoundation.nova.feature_push_notifications.data.di

import io.novafoundation.nova.feature_push_notifications.data.data.PushNotificationsService
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.PushNotificationsInteractor

interface PushNotificationsFeatureApi {

    fun pushNotificationService(): PushNotificationsService

    fun pushNotificationInteractor(): PushNotificationsInteractor
}
