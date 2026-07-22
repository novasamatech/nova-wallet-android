package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.unstake.setup.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.BittensorDelegatesClient
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorValidatorProvider
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.unstake.setup.SubtensorUnstakeSetupViewModel
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryTokenUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import java.math.BigInteger

@Module(includes = [ViewModelModule::class])
class SubtensorUnstakeSetupModule {

    @Provides
    @IntoMap
    @ViewModelKey(SubtensorUnstakeSetupViewModel::class)
    fun provideViewModel(
        router: StakingRouter,
        dashboardRouter: StakingDashboardRouter,
        resourceManager: ResourceManager,
        stakingSharedState: StakingSharedState,
        extrinsicService: ExtrinsicService,
        delegatesClient: BittensorDelegatesClient,
        validatorProvider: SubtensorValidatorProvider,
        addressIconGenerator: AddressIconGenerator,
        assetIconProvider: AssetIconProvider,
        arbitraryTokenUseCase: ArbitraryTokenUseCase,
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
        netuid: Int,
        hotkeyAddress: String,
        positionPlanks: BigInteger,
    ): ViewModel = SubtensorUnstakeSetupViewModel(
        router = router,
        dashboardRouter = dashboardRouter,
        resourceManager = resourceManager,
        stakingSharedState = stakingSharedState,
        extrinsicService = extrinsicService,
        delegatesClient = delegatesClient,
        validatorProvider = validatorProvider,
        addressIconGenerator = addressIconGenerator,
        assetIconProvider = assetIconProvider,
        arbitraryTokenUseCase = arbitraryTokenUseCase,
        feeLoaderMixinFactory = feeLoaderMixinFactory,
        netuid = netuid,
        hotkeyAddress = hotkeyAddress,
        positionPlanks = positionPlanks,
    )

    @Provides
    fun provideViewModelInstance(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): SubtensorUnstakeSetupViewModel =
        ViewModelProvider(fragment, viewModelFactory).get(SubtensorUnstakeSetupViewModel::class.java)
}
