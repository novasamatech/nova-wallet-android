package io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollator.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.modules.Caching
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.recommendations.MythosCollatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.SelectMythosInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.MythosCollatorFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollator.SelectMythosCollatorViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.SelectCollatorViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenCommunicator
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase

@Module(includes = [ViewModelModule::class])
class SelectMythosCollatorModule {

    @Provides
    @IntoMap
    @ViewModelKey(SelectMythosCollatorViewModel::class)
    fun provideViewModel(
        router: MythosStakingRouter,
        recommendatorFactory: MythosCollatorRecommendatorFactory,
        resourceManager: ResourceManager,
        tokenUseCase: TokenUseCase,
        selectedAssetState: StakingSharedState,
        mythosCollatorFormatter: MythosCollatorFormatter,
        selectCollatorInterScreenCommunicator: SelectMythosInterScreenCommunicator
    ): ViewModel {
        return SelectMythosCollatorViewModel(
            router = router,
            recommendatorFactory = recommendatorFactory,
            resourceManager = resourceManager,
            tokenUseCase = tokenUseCase,
            selectedAssetState = selectedAssetState,
            mythosCollatorFormatter = mythosCollatorFormatter,
            selectCollatorResponder = selectCollatorInterScreenCommunicator
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SelectMythosCollatorViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectMythosCollatorViewModel::class.java)
    }
}
