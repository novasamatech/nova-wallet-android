package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.warning.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.condition.ConditionMixinFactory
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common.ManualBackupAccountToBackupPayload
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.warning.ManualBackupWarningViewModel

@Module(includes = [ViewModelModule::class])
class ManualBackupWarningModule {

    @Provides
    @IntoMap
    @ViewModelKey(ManualBackupWarningViewModel::class)
    fun provideViewModel(
        resourceManager: ResourceManager,
        router: AccountRouter,
        conditionMixinFactory: ConditionMixinFactory,
        payload: ManualBackupAccountToBackupPayload
    ): ViewModel {
        return ManualBackupWarningViewModel(
            resourceManager,
            router,
            conditionMixinFactory,
            payload
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ManualBackupWarningViewModel {
        return ViewModelProvider(
            fragment,
            viewModelFactory
        ).get(ManualBackupWarningViewModel::class.java)
    }
}
