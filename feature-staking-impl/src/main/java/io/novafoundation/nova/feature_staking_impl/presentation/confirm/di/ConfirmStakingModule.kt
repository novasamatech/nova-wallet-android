package io.novafoundation.nova.feature_staking_impl.presentation.confirm.di

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
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.setup.SetupStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.confirm.ConfirmStakingViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class ConfirmStakingModule {

    @Provides
    @IntoMap
    @ViewModelKey(ConfirmStakingViewModel::class)
    fun provideViewModel(
        interactor: StakingInteractor,
        router: StakingRouter,
        addressIconGenerator: AddressIconGenerator,
        resourceManager: ResourceManager,
        addressDisplayUseCase: AddressDisplayUseCase,
        setupStakingInteractor: SetupStakingInteractor,
        validationSystem: ValidationSystem<SetupStakingPayload, SetupStakingValidationFailure>,
        validationExecutor: ValidationExecutor,
        setupStakingSharedState: SetupStakingSharedState,
        feeLoaderMixin: FeeLoaderMixin.Presentation,
        externalActions: ExternalActions.Presentation,
        singleAssetSharedState: StakingSharedState,
    ): ViewModel {
        return ConfirmStakingViewModel(
            router,
            interactor,
            addressIconGenerator,
            addressDisplayUseCase,
            resourceManager,
            validationSystem,
            setupStakingSharedState,
            setupStakingInteractor,
            feeLoaderMixin,
            externalActions,
            singleAssetSharedState,
            validationExecutor
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ConfirmStakingViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ConfirmStakingViewModel::class.java)
    }
}
