package io.novafoundation.nova.feature_push_notifications.presentation.multisigs.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_push_notifications.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.presentation.multisigs.PushMultisigSettingsCommunicator
import io.novafoundation.nova.feature_push_notifications.presentation.multisigs.PushMultisigSettingsRequester
import io.novafoundation.nova.feature_push_notifications.presentation.multisigs.PushMultisigSettingsViewModel

@Module(includes = [ViewModelModule::class])
class PushMultisigSettingsModule {

    @Provides
    @IntoMap
    @ViewModelKey(PushMultisigSettingsViewModel::class)
    fun provideViewModel(
        router: PushNotificationsRouter,
        pushMultisigSettingsCommunicator: PushMultisigSettingsCommunicator,
        request: PushMultisigSettingsRequester.Request,
        appLinksProvider: AppLinksProvider
    ): ViewModel {
        return PushMultisigSettingsViewModel(
            router = router,
            pushMultisigSettingsResponder = pushMultisigSettingsCommunicator,
            request = request,
            appLinksProvider = appLinksProvider
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): PushMultisigSettingsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(PushMultisigSettingsViewModel::class.java)
    }
}
