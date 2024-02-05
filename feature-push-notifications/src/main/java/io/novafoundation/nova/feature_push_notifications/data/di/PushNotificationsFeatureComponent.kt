package io.novafoundation.nova.feature_push_notifications.data.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.data.data.PushNotificationsService

@Component(
    dependencies = [
        PushNotificationsFeatureDependencies::class
    ],
    modules = [
        PushNotificationsFeatureModule::class
    ]
)
@FeatureScope
interface PushNotificationsFeatureComponent : PushNotificationsFeatureApi {

    fun getPushNotificationService(): PushNotificationsService

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance router: PushNotificationsRouter,
            deps: PushNotificationsFeatureDependencies
        ): PushNotificationsFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class
        ]
    )
    interface PushNotificationsFeatureDependenciesComponent : PushNotificationsFeatureDependencies
}
