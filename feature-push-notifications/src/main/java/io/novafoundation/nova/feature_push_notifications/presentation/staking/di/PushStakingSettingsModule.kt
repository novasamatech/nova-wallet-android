package io.novafoundation.nova.feature_push_notifications.presentation.staking.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_push_notifications.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.domain.interactor.StakingPushSettingsInteractor
import io.novafoundation.nova.feature_push_notifications.presentation.staking.PushStakingSettingsCommunicator
import io.novafoundation.nova.feature_push_notifications.presentation.staking.PushStakingSettingsRequester
import io.novafoundation.nova.feature_push_notifications.presentation.staking.PushStakingSettingsViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class PushStakingSettingsModule {

    @Provides
    @IntoMap
    @ViewModelKey(PushStakingSettingsViewModel::class)
    fun provideViewModel(
        router: PushNotificationsRouter,
        pushStakingSettingsCommunicator: PushStakingSettingsCommunicator,
        chainRegistry: ChainRegistry,
        request: PushStakingSettingsRequester.Request,
        resourceManager: ResourceManager,
        stakingPushSettingsInteractor: StakingPushSettingsInteractor
    ): ViewModel {
        return PushStakingSettingsViewModel(
            router = router,
            pushStakingSettingsResponder = pushStakingSettingsCommunicator,
            chainRegistry = chainRegistry,
            request = request,
            resourceManager = resourceManager,
            stakingPushSettingsInteractor = stakingPushSettingsInteractor
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): PushStakingSettingsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(PushStakingSettingsViewModel::class.java)
    }
}
