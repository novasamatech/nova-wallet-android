package io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.confirm.di

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
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.UnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.unbond.UnbondValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.confirm.ConfirmUnbondPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.confirm.ConfirmUnbondViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.hints.UnbondHintsMixinFactory

@Module(includes = [ViewModelModule::class])
class ConfirmUnbondModule {

    @Provides
    @IntoMap
    @ViewModelKey(ConfirmUnbondViewModel::class)
    fun provideViewModel(
        interactor: StakingInteractor,
        router: StakingRouter,
        unbondInteractor: UnbondInteractor,
        resourceManager: ResourceManager,
        validationExecutor: ValidationExecutor,
        validationSystem: UnbondValidationSystem,
        iconGenerator: AddressIconGenerator,
        externalActions: ExternalActions.Presentation,
        payload: ConfirmUnbondPayload,
        singleAssetSharedState: StakingSharedState,
        unbondHintsMixinFactory: UnbondHintsMixinFactory,
        walletUiUseCase: WalletUiUseCase,
    ): ViewModel {
        return ConfirmUnbondViewModel(
            router = router,
            interactor = interactor,
            unbondInteractor = unbondInteractor,
            resourceManager = resourceManager,
            validationExecutor = validationExecutor,
            iconGenerator = iconGenerator,
            validationSystem = validationSystem,
            externalActions = externalActions,
            payload = payload,
            selectedAssetState = singleAssetSharedState,
            unbondHintsMixinFactory = unbondHintsMixinFactory,
            walletUiUseCase = walletUiUseCase
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ConfirmUnbondViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ConfirmUnbondViewModel::class.java)
    }
}
