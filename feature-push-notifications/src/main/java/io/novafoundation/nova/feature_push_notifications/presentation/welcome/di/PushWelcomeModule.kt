package io.novafoundation.nova.feature_push_notifications.presentation.welcome.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.feature_push_notifications.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.domain.interactor.PushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.WelcomePushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.presentation.welcome.PushWelcomeViewModel

@Module(includes = [ViewModelModule::class])
class PushWelcomeModule {

    @Provides
    fun providePermissionAsker(
        permissionsAskerFactory: PermissionsAskerFactory,
        fragment: Fragment,
        router: PushNotificationsRouter
    ) = permissionsAskerFactory.create(fragment, router)

    @Provides
    @IntoMap
    @ViewModelKey(PushWelcomeViewModel::class)
    fun provideViewModel(
        router: PushNotificationsRouter,
        interactor: PushNotificationsInteractor,
        permissionsAsker: PermissionsAsker.Presentation,
        resourceManager: ResourceManager,
        welcomePushNotificationsInteractor: WelcomePushNotificationsInteractor
    ): ViewModel {
        return PushWelcomeViewModel(
            router,
            interactor,
            welcomePushNotificationsInteractor,
            permissionsAsker,
            resourceManager
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): PushWelcomeViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(PushWelcomeViewModel::class.java)
    }
}
