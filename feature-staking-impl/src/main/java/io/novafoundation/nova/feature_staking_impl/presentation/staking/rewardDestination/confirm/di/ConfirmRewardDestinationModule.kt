package io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.confirm.di

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
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.rewardDestination.ChangeRewardDestinationInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.rewardDestination.RewardDestinationValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.confirm.ConfirmRewardDestinationViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.confirm.parcel.ConfirmRewardDestinationPayload

@Module(includes = [ViewModelModule::class])
class ConfirmRewardDestinationModule {

    @Provides
    @IntoMap
    @ViewModelKey(ConfirmRewardDestinationViewModel::class)
    fun provideViewModel(
        interactor: StakingInteractor,
        router: StakingRouter,
        addressIconGenerator: AddressIconGenerator,
        resourceManager: ResourceManager,
        validationSystem: RewardDestinationValidationSystem,
        validationExecutor: ValidationExecutor,
        rewardDestinationInteractor: ChangeRewardDestinationInteractor,
        externalActions: ExternalActions.Presentation,
        payload: ConfirmRewardDestinationPayload,
        singleAssetSharedState: StakingSharedState,
        walletUiUseCase: WalletUiUseCase,
        extrinsicNavigationWrapper: ExtrinsicNavigationWrapper
    ): ViewModel {
        return ConfirmRewardDestinationViewModel(
            router = router,
            interactor = interactor,
            addressIconGenerator = addressIconGenerator,
            resourceManager = resourceManager,
            validationSystem = validationSystem,
            rewardDestinationInteractor = rewardDestinationInteractor,
            externalActions = externalActions,
            validationExecutor = validationExecutor,
            payload = payload,
            selectedAssetState = singleAssetSharedState,
            walletUiUseCase = walletUiUseCase,
            extrinsicNavigationWrapper = extrinsicNavigationWrapper
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ConfirmRewardDestinationViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ConfirmRewardDestinationViewModel::class.java)
    }
}
