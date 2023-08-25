package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.StartMultiStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.selectionType.MultiStakingSelectionTypeProviderFactory
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.common.PoolDisplayFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.di.CommonMultiStakingModule
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.ConfirmMultiStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.ConfirmMultiStakingViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.types.ConfirmMultiStakingTypeFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.types.RealConfirmMultiStakingTypeFactory
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase

@Module(includes = [ViewModelModule::class, CommonMultiStakingModule::class])
class ConfirmMultiStakingModule {

    @Provides
    @ScreenScope
    fun provideConfirmMultiStakingTypeFactory(
        router: StartMultiStakingRouter,
        setupStakingSharedState: SetupStakingSharedState,
        resourceManager: ResourceManager,
        poolDisplayFormatter: PoolDisplayFormatter,
    ): ConfirmMultiStakingTypeFactory {
        return RealConfirmMultiStakingTypeFactory(
            router = router,
            setupStakingSharedState = setupStakingSharedState,
            resourceManager = resourceManager,
            poolDisplayFormatter = poolDisplayFormatter
        )
    }

    @Provides
    @IntoMap
    @ViewModelKey(ConfirmMultiStakingViewModel::class)
    fun provideViewModel(
        router: StartMultiStakingRouter,
        interactor: StartMultiStakingInteractor,
        resourceManager: ResourceManager,
        validationExecutor: ValidationExecutor,
        externalActions: ExternalActions.Presentation,
        payload: ConfirmMultiStakingPayload,
        selectionStoreProvider: StartMultiStakingSelectionStoreProvider,
        confirmMultiStakingTypeFactory: ConfirmMultiStakingTypeFactory,
        assetUseCase: ArbitraryAssetUseCase,
        walletUiUseCase: WalletUiUseCase,
        selectedAccountUseCase: SelectedAccountUseCase,
        selectionTypeProviderFactory: MultiStakingSelectionTypeProviderFactory,
    ): ViewModel {
        return ConfirmMultiStakingViewModel(
            router = router,
            interactor = interactor,
            resourceManager = resourceManager,
            validationExecutor = validationExecutor,
            externalActions = externalActions,
            payload = payload,
            selectionStoreProvider = selectionStoreProvider,
            confirmMultiStakingTypeFactory = confirmMultiStakingTypeFactory,
            assetUseCase = assetUseCase,
            walletUiUseCase = walletUiUseCase,
            selectedAccountUseCase = selectedAccountUseCase,
            selectionTypeProviderFactory = selectionTypeProviderFactory
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ConfirmMultiStakingViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ConfirmMultiStakingViewModel::class.java)
    }
}
