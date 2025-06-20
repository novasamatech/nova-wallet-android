package io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.bond.BondMoreInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.bond.BondMoreValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm.ConfirmBondMorePayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm.ConfirmBondMoreViewModel

@Module(includes = [ViewModelModule::class])
class ConfirmBondMoreModule {

    @Provides
    @IntoMap
    @ViewModelKey(ConfirmBondMoreViewModel::class)
    fun provideViewModel(
        interactor: StakingInteractor,
        router: StakingRouter,
        bondMoreInteractor: BondMoreInteractor,
        resourceManager: ResourceManager,
        validationExecutor: ValidationExecutor,
        validationSystem: BondMoreValidationSystem,
        iconGenerator: AddressIconGenerator,
        externalActions: ExternalActions.Presentation,
        payload: ConfirmBondMorePayload,
        singleAssetSharedState: StakingSharedState,
        walletUiUseCase: WalletUiUseCase,
        hintsMixinFactory: ResourcesHintsMixinFactory,
        extrinsicNavigationWrapper: ExtrinsicNavigationWrapper
    ): ViewModel {
        return ConfirmBondMoreViewModel(
            router = router,
            interactor = interactor,
            bondMoreInteractor = bondMoreInteractor,
            resourceManager = resourceManager,
            validationExecutor = validationExecutor,
            iconGenerator = iconGenerator,
            validationSystem = validationSystem,
            externalActions = externalActions,
            payload = payload,
            selectedAssetState = singleAssetSharedState,
            walletUiUseCase = walletUiUseCase,
            hintsMixinFactory = hintsMixinFactory,
            extrinsicNavigationWrapper = extrinsicNavigationWrapper
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ConfirmBondMoreViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ConfirmBondMoreViewModel::class.java)
    }
}
