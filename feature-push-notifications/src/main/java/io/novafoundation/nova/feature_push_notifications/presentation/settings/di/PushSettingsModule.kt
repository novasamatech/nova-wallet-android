package io.novafoundation.nova.feature_push_notifications.presentation.settings.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectMultipleWalletsCommunicator
import io.novafoundation.nova.feature_push_notifications.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.domain.interactor.PushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.presentation.governance.PushGovernanceSettingsCommunicator
import io.novafoundation.nova.feature_push_notifications.presentation.multisigs.PushMultisigSettingsCommunicator
import io.novafoundation.nova.feature_push_notifications.presentation.multisigs.PushMultisigSettingsRequester
import io.novafoundation.nova.feature_push_notifications.presentation.settings.PushSettingsViewModel
import io.novafoundation.nova.feature_push_notifications.presentation.staking.PushStakingSettingsCommunicator

@Module(includes = [ViewModelModule::class])
class PushSettingsModule {

    @Provides
    fun providePermissionAsker(
        permissionsAskerFactory: PermissionsAskerFactory,
        fragment: Fragment,
        router: PushNotificationsRouter
    ) = permissionsAskerFactory.createReturnable(fragment, router)

    @Provides
    @IntoMap
    @ViewModelKey(PushSettingsViewModel::class)
    fun provideViewModel(
        router: PushNotificationsRouter,
        interactor: PushNotificationsInteractor,
        resourceManager: ResourceManager,
        selectMultipleWalletsCommunicator: SelectMultipleWalletsCommunicator,
        pushGovernanceSettingsCommunicator: PushGovernanceSettingsCommunicator,
        pushStakingSettingsRequester: PushStakingSettingsCommunicator,
        permissionsAsker: PermissionsAsker.Presentation,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        pushMultisigSettingsCommunicator: PushMultisigSettingsCommunicator,
    ): ViewModel {
        return PushSettingsViewModel(
            router,
            interactor,
            resourceManager,
            selectMultipleWalletsCommunicator,
            pushGovernanceSettingsCommunicator,
            pushStakingSettingsRequester,
            pushMultisigSettingsCommunicator,
            actionAwaitableMixinFactory,
            permissionsAsker
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): PushSettingsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(PushSettingsViewModel::class.java)
    }
}
