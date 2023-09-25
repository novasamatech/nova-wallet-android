package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.confirm.di

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
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.ParachainStakingUnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow.ParachainStakingUnbondValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.confirm.ParachainStakingUnbondConfirmViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.confirm.model.ParachainStakingUnbondConfirmPayload
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.hints.ParachainStakingUnbondHintsMixinFactory
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class ParachainStakingUnbondConfirmModule {

    @Provides
    @IntoMap
    @ViewModelKey(ParachainStakingUnbondConfirmViewModel::class)
    fun provideViewModel(
        router: ParachainStakingRouter,
        addressIconGenerator: AddressIconGenerator,
        resourceManager: ResourceManager,
        validationSystem: ParachainStakingUnbondValidationSystem,
        interactor: ParachainStakingUnbondInteractor,
        feeLoaderMixin: FeeLoaderMixin.Presentation,
        externalActions: ExternalActions.Presentation,
        selectedAssetState: StakingSharedState,
        validationExecutor: ValidationExecutor,
        collatorsUseCase: CollatorsUseCase,
        payload: ParachainStakingUnbondConfirmPayload,
        selectedAccountUseCase: SelectedAccountUseCase,
        assetUseCase: AssetUseCase,
        walletUiUseCase: WalletUiUseCase,
        hintsMixinFactory: ParachainStakingUnbondHintsMixinFactory,
    ): ViewModel {
        return ParachainStakingUnbondConfirmViewModel(
            router = router,
            addressIconGenerator = addressIconGenerator,
            selectedAccountUseCase = selectedAccountUseCase,
            resourceManager = resourceManager,
            validationSystem = validationSystem,
            interactor = interactor,
            feeLoaderMixin = feeLoaderMixin,
            externalActions = externalActions,
            selectedAssetState = selectedAssetState,
            validationExecutor = validationExecutor,
            assetUseCase = assetUseCase,
            walletUiUseCase = walletUiUseCase,
            payload = payload,
            hintsMixinFactory = hintsMixinFactory,
            collatorsUseCase = collatorsUseCase,
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ParachainStakingUnbondConfirmViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ParachainStakingUnbondConfirmViewModel::class.java)
    }
}
