package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.confirm.di

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
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.setup.ChangeValidatorsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.confirm.ConfirmChangeValidatorsViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.confirm.hints.ConfirmStakeHintsMixinFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class ConfirmChangeValidatorsModule {

    @Provides
    @ScreenScope
    fun provideConfirmStakeHintsMixinFactory(
        resourceManager: ResourceManager,
    ): ConfirmStakeHintsMixinFactory {
        return ConfirmStakeHintsMixinFactory(resourceManager)
    }

    @Provides
    @IntoMap
    @ViewModelKey(ConfirmChangeValidatorsViewModel::class)
    fun provideViewModel(
        interactor: StakingInteractor,
        router: StakingRouter,
        addressIconGenerator: AddressIconGenerator,
        resourceManager: ResourceManager,
        changeValidatorsInteractor: ChangeValidatorsInteractor,
        validationSystem: ValidationSystem<SetupStakingPayload, SetupStakingValidationFailure>,
        validationExecutor: ValidationExecutor,
        setupStakingSharedState: SetupStakingSharedState,
        feeLoaderMixin: FeeLoaderMixin.Presentation,
        externalActions: ExternalActions.Presentation,
        singleAssetSharedState: StakingSharedState,
        walletUiUseCase: WalletUiUseCase,
        hintsMixinFactory: ConfirmStakeHintsMixinFactory,
        extrinsicNavigationWrapper: ExtrinsicNavigationWrapper
    ): ViewModel {
        return ConfirmChangeValidatorsViewModel(
            router = router,
            interactor = interactor,
            addressIconGenerator = addressIconGenerator,
            resourceManager = resourceManager,
            validationSystem = validationSystem,
            setupStakingSharedState = setupStakingSharedState,
            changeValidatorsInteractor = changeValidatorsInteractor,
            feeLoaderMixin = feeLoaderMixin,
            externalActions = externalActions,
            selectedAssetState = singleAssetSharedState,
            validationExecutor = validationExecutor,
            walletUiUseCase = walletUiUseCase,
            hintsMixinFactory = hintsMixinFactory,
            extrinsicNavigationWrapper = extrinsicNavigationWrapper
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ConfirmChangeValidatorsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ConfirmChangeValidatorsViewModel::class.java)
    }
}
