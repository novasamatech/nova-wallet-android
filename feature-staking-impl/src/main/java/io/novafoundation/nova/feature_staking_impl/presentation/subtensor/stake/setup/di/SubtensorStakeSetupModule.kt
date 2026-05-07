package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.setup.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.setup.SubtensorStakeSetupViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory

@Module(includes = [ViewModelModule::class])
class SubtensorStakeSetupModule {

    @Provides
    @IntoMap
    @ViewModelKey(SubtensorStakeSetupViewModel::class)
    fun provideViewModel(
        router: StakingRouter,
        dashboardRouter: StakingDashboardRouter,
        resourceManager: ResourceManager,
        stakingSharedState: StakingSharedState,
        extrinsicService: ExtrinsicService,
        assetUseCase: AssetUseCase,
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
        maxActionProviderFactory: MaxActionProviderFactory,
        amountChooserMixinFactory: AmountChooserMixin.Factory,
        netuid: Int,
        subnetName: String?,
    ): ViewModel = SubtensorStakeSetupViewModel(
        router = router,
        dashboardRouter = dashboardRouter,
        resourceManager = resourceManager,
        stakingSharedState = stakingSharedState,
        extrinsicService = extrinsicService,
        assetUseCase = assetUseCase,
        feeLoaderMixinFactory = feeLoaderMixinFactory,
        maxActionProviderFactory = maxActionProviderFactory,
        amountChooserMixinFactory = amountChooserMixinFactory,
        netuid = netuid,
        subnetName = subnetName,
    )

    @Provides
    fun provideViewModelInstance(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): SubtensorStakeSetupViewModel = ViewModelProvider(fragment, viewModelFactory).get(SubtensorStakeSetupViewModel::class.java)
}
