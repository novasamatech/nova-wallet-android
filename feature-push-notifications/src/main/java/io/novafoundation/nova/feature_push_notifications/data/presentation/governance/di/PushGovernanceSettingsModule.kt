package io.novafoundation.nova.feature_push_notifications.data.presentation.governance.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksCommunicator
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.GovernancePushSettingsInteractor
import io.novafoundation.nova.feature_push_notifications.data.presentation.governance.PushGovernanceSettingsCommunicator
import io.novafoundation.nova.feature_push_notifications.data.presentation.governance.PushGovernanceSettingsRequester
import io.novafoundation.nova.feature_push_notifications.data.presentation.governance.PushGovernanceSettingsViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class PushGovernanceSettingsModule {

    @Provides
    @IntoMap
    @ViewModelKey(PushGovernanceSettingsViewModel::class)
    fun provideViewModel(
        router: PushNotificationsRouter,
        interactor: GovernancePushSettingsInteractor,
        pushGovernanceSettingsCommunicator: PushGovernanceSettingsCommunicator,
        chainRegistry: ChainRegistry,
        request: PushGovernanceSettingsRequester.Request,
        selectTracksCommunicator: SelectTracksCommunicator,
        resourceManager: ResourceManager
    ): ViewModel {
        return PushGovernanceSettingsViewModel(
            router = router,
            interactor = interactor,
            pushGovernanceSettingsResponder = pushGovernanceSettingsCommunicator,
            chainRegistry = chainRegistry,
            request = request,
            selectTracksRequester = selectTracksCommunicator,
            resourceManager = resourceManager
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): PushGovernanceSettingsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(PushGovernanceSettingsViewModel::class.java)
    }
}
