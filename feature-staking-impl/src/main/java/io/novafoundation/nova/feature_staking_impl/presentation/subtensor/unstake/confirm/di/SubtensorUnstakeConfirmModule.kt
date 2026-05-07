package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.unstake.confirm.di

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
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorPositionCache
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorSubnetFetcher
import io.novafoundation.nova.feature_staking_impl.domain.subtensor.SubtensorStakeSubmitInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.unstake.confirm.SubtensorUnstakeConfirmPayload
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.unstake.confirm.SubtensorUnstakeConfirmViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter

@Module(includes = [ViewModelModule::class])
class SubtensorUnstakeConfirmModule {

    @Provides
    fun provideSubmitInteractor(
        extrinsicService: ExtrinsicService,
        stakingSharedState: StakingSharedState,
        accountRepository: AccountRepository,
        subnetFetcher: SubtensorSubnetFetcher,
        positionCache: SubtensorPositionCache,
    ): SubtensorStakeSubmitInteractor = SubtensorStakeSubmitInteractor(
        extrinsicService = extrinsicService,
        stakingSharedState = stakingSharedState,
        accountRepository = accountRepository,
        subnetFetcher = subnetFetcher,
        positionCache = positionCache,
    )

    @Provides
    @IntoMap
    @ViewModelKey(SubtensorUnstakeConfirmViewModel::class)
    fun provideViewModel(
        router: StakingRouter,
        dashboardRouter: StakingDashboardRouter,
        resourceManager: ResourceManager,
        stakingSharedState: StakingSharedState,
        accountRepository: AccountRepository,
        submitInteractor: SubtensorStakeSubmitInteractor,
        assetUseCase: AssetUseCase,
        subnetFetcher: SubtensorSubnetFetcher,
        addressIconGenerator: AddressIconGenerator,
        walletUiUseCase: WalletUiUseCase,
        amountFormatter: AmountFormatter,
        externalActions: ExternalActions.Presentation,
        payload: SubtensorUnstakeConfirmPayload,
    ): ViewModel = SubtensorUnstakeConfirmViewModel(
        router = router,
        dashboardRouter = dashboardRouter,
        resourceManager = resourceManager,
        stakingSharedState = stakingSharedState,
        accountRepository = accountRepository,
        submitInteractor = submitInteractor,
        assetUseCase = assetUseCase,
        subnetFetcher = subnetFetcher,
        addressIconGenerator = addressIconGenerator,
        walletUiUseCase = walletUiUseCase,
        amountFormatter = amountFormatter,
        externalActions = externalActions,
        payload = payload,
    )

    @Provides
    fun provideViewModelInstance(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): SubtensorUnstakeConfirmViewModel =
        ViewModelProvider(fragment, viewModelFactory).get(SubtensorUnstakeConfirmViewModel::class.java)
}
