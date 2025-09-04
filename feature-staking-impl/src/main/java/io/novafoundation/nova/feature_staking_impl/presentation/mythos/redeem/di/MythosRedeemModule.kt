package io.novafoundation.nova.feature_staking_impl.presentation.mythos.redeem.di

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
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.domain.mythos.redeem.MythosRedeemInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.redeem.validations.RedeemMythosValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.validations.MythosStakingValidationFailureFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.redeem.MythosRedeemViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState

@Module(includes = [ViewModelModule::class])
class MythosRedeemModule {

    @Provides
    @IntoMap
    @ViewModelKey(MythosRedeemViewModel::class)
    fun provideViewModel(
        router: MythosStakingRouter,
        resourceManager: ResourceManager,
        validationSystem: RedeemMythosValidationSystem,
        validationFailureFormatter: MythosStakingValidationFailureFormatter,
        interactor: MythosRedeemInteractor,
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
        externalActions: ExternalActions.Presentation,
        selectedAssetState: AnySelectedAssetOptionSharedState,
        validationExecutor: ValidationExecutor,
        selectedAccountUseCase: SelectedAccountUseCase,
        assetUseCase: AssetUseCase,
        walletUiUseCase: WalletUiUseCase,
    ): ViewModel {
        return MythosRedeemViewModel(
            router = router,
            resourceManager = resourceManager,
            validationSystem = validationSystem,
            validationFailureFormatter = validationFailureFormatter,
            interactor = interactor,
            feeLoaderMixinV2Factory = feeLoaderMixinFactory,
            externalActions = externalActions,
            selectedAssetState = selectedAssetState,
            validationExecutor = validationExecutor,
            selectedAccountUseCase = selectedAccountUseCase,
            assetUseCase = assetUseCase,
            walletUiUseCase = walletUiUseCase
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): MythosRedeemViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(MythosRedeemViewModel::class.java)
    }
}
