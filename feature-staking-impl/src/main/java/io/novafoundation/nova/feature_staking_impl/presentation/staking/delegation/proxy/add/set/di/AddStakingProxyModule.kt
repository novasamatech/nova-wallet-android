package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.add.set.di

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
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectAddressCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_staking_api.data.proxy.AddStakingProxyRepository
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.AddStakingProxyValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.add.set.AddStakingProxyViewModel
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState

@Module(includes = [ViewModelModule::class])
class AddStakingProxyModule {

    @Provides
    @IntoMap
    @ViewModelKey(AddStakingProxyViewModel::class)
    fun provideViewModel(
        addressInputMixinFactory: AddressInputMixinFactory,
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        selectedAssetState: AnySelectedAssetOptionSharedState,
        externalActions: ExternalActions.Presentation,
        interactor: StakingInteractor,
        accountRepository: AccountRepository,
        assetUseCase: ArbitraryAssetUseCase,
        resourceManager: ResourceManager,
        selectAddressCommunicator: SelectAddressCommunicator,
        addStakingProxyRepository: AddStakingProxyRepository,
        validationExecutor: ValidationExecutor,
        addStakingProxyValidationSystem: AddStakingProxyValidationSystem,
        descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher,
        metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
        stakingRouter: StakingRouter
    ): ViewModel {
        return AddStakingProxyViewModel(
            addressInputMixinFactory = addressInputMixinFactory,
            feeLoaderMixinFactory = feeLoaderMixinFactory,
            selectedAssetState = selectedAssetState,
            externalActions = externalActions,
            interactor = interactor,
            accountRepository = accountRepository,
            assetUseCase = assetUseCase,
            resourceManager = resourceManager,
            selectAddressRequester = selectAddressCommunicator,
            addStakingProxyRepository = addStakingProxyRepository,
            validationExecutor = validationExecutor,
            addStakingProxyValidationSystem = addStakingProxyValidationSystem,
            descriptionBottomSheetLauncher = descriptionBottomSheetLauncher,
            metaAccountGroupingInteractor = metaAccountGroupingInteractor,
            stakingRouter = stakingRouter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): AddStakingProxyViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(AddStakingProxyViewModel::class.java)
    }
}
