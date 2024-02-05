package io.novafoundation.nova.feature_push_notifications.data.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsRouter
import javax.inject.Inject

class PushNotificationsFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val router: PushNotificationsRouter
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val dependencies = DaggerPushNotificationsFeatureComponent_PushNotificationsFeatureComponent.builder()
            .commonApi(commonApi())
            .build()
        return DaggerPushNotificationsFeatureComponent.factory()
            .create(router, dependencies)
    }
}
