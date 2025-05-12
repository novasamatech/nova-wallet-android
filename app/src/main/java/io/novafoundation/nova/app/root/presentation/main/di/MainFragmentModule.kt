package io.novafoundation.nova.app.root.presentation.main.di

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.app.root.presentation.RootRouter
import io.novafoundation.nova.app.root.presentation.main.MainViewModel
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_push_notifications.domain.interactor.WelcomePushNotificationsInteractor
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor

@Module(
    includes = [
        ViewModelModule::class
    ]
)
class MainFragmentModule {

    @Provides
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    fun provideViewModel(
        updateNotificationsInteractor: UpdateNotificationsInteractor,
        automaticInteractionGate: AutomaticInteractionGate,
        welcomePushNotificationsInteractor: WelcomePushNotificationsInteractor,
        rootRouter: RootRouter
    ): ViewModel {
        return MainViewModel(
            updateNotificationsInteractor,
            automaticInteractionGate,
            welcomePushNotificationsInteractor,
            rootRouter
        )
    }

    @Provides
    fun provideViewModelCreator(
        activity: FragmentActivity,
        viewModelFactory: ViewModelProvider.Factory
    ): MainViewModel {
        return ViewModelProvider(activity, viewModelFactory).get(MainViewModel::class.java)
    }
}
