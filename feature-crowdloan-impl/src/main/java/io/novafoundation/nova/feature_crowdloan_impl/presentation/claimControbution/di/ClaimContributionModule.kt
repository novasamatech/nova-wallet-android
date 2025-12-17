package io.novafoundation.nova.feature_crowdloan_impl.presentation.claimControbution.di

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
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_crowdloan_impl.domain.claimContributions.ClaimContributionsInteractor
import io.novafoundation.nova.feature_crowdloan_impl.domain.claimContributions.validation.ClaimContributionValidationSystem
import io.novafoundation.nova.feature_crowdloan_impl.domain.claimContributions.validation.claimContribution
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.claimControbution.ClaimContributionViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState

@Module(includes = [ViewModelModule::class])
class ClaimContributionModule {

    @ScreenScope
    @Provides
    fun provideValidationSystem() = ValidationSystem.claimContribution()

    @Provides
    @IntoMap
    @ViewModelKey(ClaimContributionViewModel::class)
    fun provideViewModel(
        router: CrowdloanRouter,
        resourceManager: ResourceManager,
        validationSystem: ClaimContributionValidationSystem,
        interactor: ClaimContributionsInteractor,
        feeLoaderMixinV2Factory: FeeLoaderMixinV2.Factory,
        externalActions: ExternalActions.Presentation,
        selectedAssetState: AnySelectedAssetOptionSharedState,
        validationExecutor: ValidationExecutor,
        extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
        selectedAccountUseCase: SelectedAccountUseCase,
        assetUseCase: AssetUseCase,
        walletUiUseCase: WalletUiUseCase,
        amountFormatter: AmountFormatter
    ): ViewModel {
        return ClaimContributionViewModel(
            router = router,
            resourceManager = resourceManager,
            validationSystem = validationSystem,
            interactor = interactor,
            feeLoaderMixinV2Factory = feeLoaderMixinV2Factory,
            externalActions = externalActions,
            selectedAssetState = selectedAssetState,
            validationExecutor = validationExecutor,
            selectedAccountUseCase = selectedAccountUseCase,
            assetUseCase = assetUseCase,
            walletUiUseCase = walletUiUseCase,
            extrinsicNavigationWrapper = extrinsicNavigationWrapper,
            amountFormatter = amountFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ClaimContributionViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ClaimContributionViewModel::class.java)
    }
}
