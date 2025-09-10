package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.search.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.collator.search.SearchCollatorsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.search.SearchCollatorViewModel
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter

@Module(includes = [ViewModelModule::class])
class SearchCollatorValidatorsModule {

    @Provides
    @ScreenScope
    fun provideInteractor() = SearchCollatorsInteractor()

    @Provides
    @IntoMap
    @ViewModelKey(SearchCollatorViewModel::class)
    fun provideViewModel(
        router: ParachainStakingRouter,
        interactor: SearchCollatorsInteractor,
        addressIconGenerator: AddressIconGenerator,
        stakingSharedState: StakingSharedState,
        selectCollatorInterScreenResponder: SelectCollatorInterScreenCommunicator,
        collatorRecommendatorFactory: CollatorRecommendatorFactory,
        collatorsUseCase: CollatorsUseCase,
        resourceManager: ResourceManager,
        tokenUseCase: TokenUseCase,
        amountFormatter: AmountFormatter
    ): ViewModel {
        return SearchCollatorViewModel(
            router = router,
            interactor = interactor,
            addressIconGenerator = addressIconGenerator,
            singleAssetSharedState = stakingSharedState,
            selectCollatorInterScreenResponder = selectCollatorInterScreenResponder,
            collatorRecommendatorFactory = collatorRecommendatorFactory,
            collatorsUseCase = collatorsUseCase,
            resourceManager = resourceManager,
            tokenUseCase = tokenUseCase,
            amountFormatter = amountFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SearchCollatorViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SearchCollatorViewModel::class.java)
    }
}
