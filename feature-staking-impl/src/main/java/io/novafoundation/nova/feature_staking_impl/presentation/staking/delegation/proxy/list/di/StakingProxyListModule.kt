package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.list.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.list.RealStakingProxyListInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.list.StakingProxyListInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.list.StakingProxyListViewModel
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState

@Module(includes = [ViewModelModule::class])
class StakingProxyListModule {

    @Provides
    @IntoMap
    @ViewModelKey(StakingProxyListViewModel::class)
    fun provideViewModel(
        selectedAssetState: AnySelectedAssetOptionSharedState,
        externalActions: ExternalActions.Presentation,
        accountRepository: AccountRepository,
        resourceManager: ResourceManager,
        stakingRouter: StakingRouter,
        addressIconGenerator: AddressIconGenerator,
        stakingProxyListInteractor: StakingProxyListInteractor
    ): ViewModel {
        return StakingProxyListViewModel(
            selectedAssetState = selectedAssetState,
            externalActions = externalActions,
            accountRepository = accountRepository,
            resourceManager = resourceManager,
            stakingRouter = stakingRouter,
            addressIconGenerator = addressIconGenerator,
            stakingProxyListInteractor = stakingProxyListInteractor
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): StakingProxyListViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(StakingProxyListViewModel::class.java)
    }
}
