package io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings.SelectMythCollatorSettingsInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings.SelectMythCollatorSettingsViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings.model.MythCollatorRecommendationConfigParcel

@Module(includes = [ViewModelModule::class])
class SelectMythCollatorSettingsModule {

    @Provides
    @IntoMap
    @ViewModelKey(SelectMythCollatorSettingsViewModel::class)
    fun provideViewModel(
        router: MythosStakingRouter,
        payload: MythCollatorRecommendationConfigParcel,
        selectCollatorSettingsInterScreenResponder: SelectMythCollatorSettingsInterScreenCommunicator,
    ): ViewModel {
        return SelectMythCollatorSettingsViewModel(
            router = router,
            payload = payload,
            selectCollatorSettingsInterScreenResponder = selectCollatorSettingsInterScreenResponder
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SelectMythCollatorSettingsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectMythCollatorSettingsViewModel::class.java)
    }
}
