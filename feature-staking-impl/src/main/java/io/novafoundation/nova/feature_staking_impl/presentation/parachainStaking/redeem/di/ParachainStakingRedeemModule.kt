package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.redeem.di

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
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.DelegatorStateRepository
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.redeem.ParachainStakingRedeemInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.redeem.RealParachainStakingRedeemInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.redeem.validations.ParachainStakingRedeemValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.redeem.validations.parachainStakingRedeem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.redeem.ParachainStakingRedeemViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class ParachainStakingRedeemModule {

    @Provides
    @ScreenScope
    fun provideValidationSystem(): ParachainStakingRedeemValidationSystem {
        return ValidationSystem.parachainStakingRedeem()
    }

    @Provides
    @ScreenScope
    fun provideInteractor(
        extrinsicService: ExtrinsicService,
        cureRoundRepository: CurrentRoundRepository,
        delegatorStateRepository: DelegatorStateRepository,
    ): ParachainStakingRedeemInteractor = RealParachainStakingRedeemInteractor(
        extrinsicService = extrinsicService,
        currentRoundRepository = cureRoundRepository,
        delegatorStateRepository = delegatorStateRepository
    )

    @Provides
    @IntoMap
    @ViewModelKey(ParachainStakingRedeemViewModel::class)
    fun provideViewModel(
        router: StakingRouter,
        addressIconGenerator: AddressIconGenerator,
        resourceManager: ResourceManager,
        validationSystem: ParachainStakingRedeemValidationSystem,
        interactor: ParachainStakingRedeemInteractor,
        feeLoaderMixin: FeeLoaderMixin.Presentation,
        externalActions: ExternalActions.Presentation,
        selectedAssetState: StakingSharedState,
        validationExecutor: ValidationExecutor,
        delegatorStateUseCase: DelegatorStateUseCase,
        selectedAccountUseCase: SelectedAccountUseCase,
        assetUseCase: AssetUseCase,
        walletUiUseCase: WalletUiUseCase,
        extrinsicNavigationWrapper: ExtrinsicNavigationWrapper
    ): ViewModel {
        return ParachainStakingRedeemViewModel(
            router = router,
            addressIconGenerator = addressIconGenerator,
            resourceManager = resourceManager,
            validationSystem = validationSystem,
            interactor = interactor,
            feeLoaderMixin = feeLoaderMixin,
            externalActions = externalActions,
            selectedAssetState = selectedAssetState,
            validationExecutor = validationExecutor,
            delegatorStateUseCase = delegatorStateUseCase,
            selectedAccountUseCase = selectedAccountUseCase,
            assetUseCase = assetUseCase,
            walletUiUseCase = walletUiUseCase,
            extrinsicNavigationWrapper = extrinsicNavigationWrapper
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ParachainStakingRedeemViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ParachainStakingRedeemViewModel::class.java)
    }
}
