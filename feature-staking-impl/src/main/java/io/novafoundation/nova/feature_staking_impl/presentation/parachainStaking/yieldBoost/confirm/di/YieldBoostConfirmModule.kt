package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm.di

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
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.YieldBoostInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.validations.YieldBoostValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm.YieldBoostConfirmViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm.model.YieldBoostConfirmPayload
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class YieldBoostConfirmModule {

    @Provides
    @IntoMap
    @ViewModelKey(YieldBoostConfirmViewModel::class)
    fun provideViewModel(
        router: ParachainStakingRouter,
        addressIconGenerator: AddressIconGenerator,
        resourceManager: ResourceManager,
        validationSystem: YieldBoostValidationSystem,
        feeLoaderMixin: FeeLoaderMixin.Presentation,
        externalActions: ExternalActions.Presentation,
        selectedAssetState: StakingSharedState,
        validationExecutor: ValidationExecutor,
        interactor: YieldBoostInteractor,
        payload: YieldBoostConfirmPayload,
        delegatorStateUseCase: DelegatorStateUseCase,
        selectedAccountUseCase: SelectedAccountUseCase,
        assetUseCase: AssetUseCase,
        walletUiUseCase: WalletUiUseCase,
        extrinsicNavigationWrapper: ExtrinsicNavigationWrapper
    ): ViewModel {
        return YieldBoostConfirmViewModel(
            router = router,
            addressIconGenerator = addressIconGenerator,
            resourceManager = resourceManager,
            validationSystem = validationSystem,
            feeLoaderMixin = feeLoaderMixin,
            externalActions = externalActions,
            selectedAssetState = selectedAssetState,
            validationExecutor = validationExecutor,
            interactor = interactor,
            payload = payload,
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
    ): YieldBoostConfirmViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(YieldBoostConfirmViewModel::class.java)
    }
}
