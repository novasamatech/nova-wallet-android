package io.novafoundation.nova.feature_staking_impl.presentation.mythos.claimRewards.di

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
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.claimRewards.MythosClaimRewardsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.claimRewards.validations.MythosClaimRewardsValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.claimRewards.MythosClaimRewardsViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.validations.MythosStakingValidationFailureFormatter
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2

@Module(includes = [ViewModelModule::class])
class MythosClaimRewardsModule {

    @Provides
    @IntoMap
    @ViewModelKey(MythosClaimRewardsViewModel::class)
    fun provideViewModel(
        router: MythosStakingRouter,
        interactor: MythosClaimRewardsInteractor,
        resourceManager: ResourceManager,
        validationExecutor: ValidationExecutor,
        validationSystem: MythosClaimRewardsValidationSystem,
        validationFailureFormatter: MythosStakingValidationFailureFormatter,
        stakingSharedState: StakingSharedState,
        externalActions: ExternalActions.Presentation,
        selectedAccountUseCase: SelectedAccountUseCase,
        walletUiUseCase: WalletUiUseCase,
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
        assetUseCase: AssetUseCase,
        extrinsicNavigationWrapper: ExtrinsicNavigationWrapper
    ): ViewModel {
        return MythosClaimRewardsViewModel(
            router = router,
            interactor = interactor,
            resourceManager = resourceManager,
            validationExecutor = validationExecutor,
            validationSystem = validationSystem,
            validationFailureFormatter = validationFailureFormatter,
            stakingSharedState = stakingSharedState,
            externalActions = externalActions,
            selectedAccountUseCase = selectedAccountUseCase,
            walletUiUseCase = walletUiUseCase,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            assetUseCase = assetUseCase,
            extrinsicNavigationWrapper = extrinsicNavigationWrapper
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): MythosClaimRewardsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(MythosClaimRewardsViewModel::class.java)
    }
}
