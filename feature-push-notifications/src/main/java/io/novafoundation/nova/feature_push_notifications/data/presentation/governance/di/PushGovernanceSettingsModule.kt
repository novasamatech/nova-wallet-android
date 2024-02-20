package io.novafoundation.nova.feature_push_notifications.data.presentation.governance.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.data.presentation.governance.PushGovernanceSettingsViewModel

@Module(includes = [ViewModelModule::class])
class PushGovernanceSettingsModule {

    @Provides
    @IntoMap
    @ViewModelKey(PushGovernanceSettingsViewModel::class)
    fun provideViewModel(router: PushNotificationsRouter): ViewModel {
        return PushGovernanceSettingsViewModel(router = router)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): PushGovernanceSettingsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(PushGovernanceSettingsViewModel::class.java)
    }
}
