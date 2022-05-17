package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.model.CollatorRecommendationConfigParcelModel

@Module(includes = [ViewModelModule::class])
class SelectCollatorSettingsModule {

    @Provides
    @IntoMap
    @ViewModelKey(SelectCollatorSettingsViewModel::class)
    fun provideViewModel(
        router: ParachainStakingRouter,
        selectCollatorSettingsInterScreenCommunicator: SelectCollatorSettingsInterScreenCommunicator,
        payload: CollatorRecommendationConfigParcelModel
    ): ViewModel {
        return SelectCollatorSettingsViewModel(
            router = router,
            selectCollatorSettingsInterScreenResponder = selectCollatorSettingsInterScreenCommunicator,
            payload = payload
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SelectCollatorSettingsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectCollatorSettingsViewModel::class.java)
    }
}
