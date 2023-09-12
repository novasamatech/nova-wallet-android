package io.novafoundation.nova.feature_staking_impl.presentation.pools.searchPool.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.selecting.SearchNominationPoolInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.SetupStakingTypeSelectionMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.common.PoolDisplayFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.SelectingPoolPayload
import io.novafoundation.nova.feature_staking_impl.presentation.pools.searchPool.SearchPoolViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class SearchPoolModule {

    @Provides
    @IntoMap
    @ViewModelKey(SearchPoolViewModel::class)
    fun provideViewModel(
        router: StakingRouter,
        setupStakingTypeSelectionMixinFactory: SetupStakingTypeSelectionMixinFactory,
        payload: SelectingPoolPayload,
        resourceManager: ResourceManager,
        selectNominationPoolInteractor: SearchNominationPoolInteractor,
        chainRegistry: ChainRegistry,
        externalActions: ExternalActions.Presentation,
        poolDisplayFormatter: PoolDisplayFormatter,
        validationExecutor: ValidationExecutor
    ): ViewModel {
        return SearchPoolViewModel(
            router,
            setupStakingTypeSelectionMixinFactory,
            payload,
            resourceManager,
            selectNominationPoolInteractor,
            chainRegistry,
            externalActions,
            poolDisplayFormatter,
            validationExecutor
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SearchPoolViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SearchPoolViewModel::class.java)
    }
}
