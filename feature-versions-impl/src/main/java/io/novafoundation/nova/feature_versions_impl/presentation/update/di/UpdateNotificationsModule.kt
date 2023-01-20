package io.novafoundation.nova.feature_versions_impl.presentation.update.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.navigation.DelayedNavigation
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor
import io.novafoundation.nova.feature_versions_api.presentation.VersionsRouter
import io.novafoundation.nova.feature_versions_impl.presentation.update.UpdateNotificationViewModel

@Module(includes = [ViewModelModule::class])
class UpdateNotificationsModule {

    @Provides
    @IntoMap
    @ViewModelKey(UpdateNotificationViewModel::class)
    fun provideViewModel(
        router: VersionsRouter,
        interactor: UpdateNotificationsInteractor,
        nextNavigation: DelayedNavigation,
        resourceManager: ResourceManager
    ): ViewModel {
        return UpdateNotificationViewModel(router, interactor, nextNavigation, resourceManager)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): UpdateNotificationViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(UpdateNotificationViewModel::class.java)
    }
}
