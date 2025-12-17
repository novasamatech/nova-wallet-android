package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.di

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
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.SelectCollatorViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenCommunicator
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter

@Module(includes = [ViewModelModule::class])
class SelectCollatorModule {

    @Provides
    @IntoMap
    @ViewModelKey(SelectCollatorViewModel::class)
    fun provideViewModel(
        router: ParachainStakingRouter,
        selectCollatorInterScreenCommunicator: SelectCollatorInterScreenCommunicator,
        selectCollatorSettingsInterScreenCommunicator: SelectCollatorSettingsInterScreenCommunicator,
        collatorRecommendatorFactory: CollatorRecommendatorFactory,
        collatorsUseCase: CollatorsUseCase,
        @Caching addressIconGenerator: AddressIconGenerator,
        resourceManager: ResourceManager,
        tokenUseCase: TokenUseCase,
        selectedAssetState: StakingSharedState,
        amountFormatter: AmountFormatter
    ): ViewModel {
        return SelectCollatorViewModel(
            router = router,
            selectCollatorInterScreenResponder = selectCollatorInterScreenCommunicator,
            collatorRecommendatorFactory = collatorRecommendatorFactory,
            addressIconGenerator = addressIconGenerator,
            resourceManager = resourceManager,
            tokenUseCase = tokenUseCase,
            selectedAssetState = selectedAssetState,
            collatorsUseCase = collatorsUseCase,
            selectCollatorSettingsInterScreenRequester = selectCollatorSettingsInterScreenCommunicator,
            amountFormatter = amountFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SelectCollatorViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectCollatorViewModel::class.java)
    }
}
