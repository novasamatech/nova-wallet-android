package io.novafoundation.nova.feature_push_notifications.data.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.data.data.PushNotificationsService
import io.novafoundation.nova.feature_push_notifications.data.presentation.welcome.di.PushWelcomeComponent
import io.novafoundation.nova.runtime.di.RuntimeApi

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

    fun pushWelcomeComponentFactory(): PushWelcomeComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance router: PushNotificationsRouter,
            deps: PushNotificationsFeatureDependencies
        ): PushNotificationsFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            RuntimeApi::class
        ]
    )
    interface PushNotificationsFeatureDependenciesComponent : PushNotificationsFeatureDependencies
}
