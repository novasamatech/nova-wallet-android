package io.novafoundation.nova.feature_staking_impl.presentation.pools.selectPool.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.KnownNovaPools
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.pools.NominationPoolProvider
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.pools.recommendation.NominationPoolRecommenderFactory
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.selecting.SearchNominationPoolInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.SetupStakingTypeSelectionMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.common.PoolDisplayFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.SelectingPoolPayload
import io.novafoundation.nova.feature_staking_impl.presentation.pools.selectPool.SelectPoolViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class SelectPoolModule {

    @Provides
    @IntoMap
    @ViewModelKey(SelectPoolViewModel::class)
    fun provideViewModel(
        stakingRouter: StakingRouter,
        selectNominationPoolInteractor: SearchNominationPoolInteractor,
        nominationPoolRecommenderFactory: NominationPoolRecommenderFactory,
        setupStakingTypeSelectionMixinFactory: SetupStakingTypeSelectionMixinFactory,
        payload: SelectingPoolPayload,
        resourceManager: ResourceManager,
        chainRegistry: ChainRegistry,
        poolDisplayFormatter: PoolDisplayFormatter,
        externalActions: ExternalActions.Presentation
    ): ViewModel {
        return SelectPoolViewModel(
            stakingRouter,
            nominationPoolRecommenderFactory,
            setupStakingTypeSelectionMixinFactory,
            payload,
            resourceManager,
            selectNominationPoolInteractor,
            chainRegistry,
            externalActions,
            poolDisplayFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): SelectPoolViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(SelectPoolViewModel::class.java)
    }
}
