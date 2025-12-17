package io.novafoundation.nova.feature_crowdloan_impl.presentation.main.di

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
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_crowdloan_impl.data.CrowdloanSharedState
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeManager
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.CrowdloanInteractor
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.statefull.StatefulCrowdloanMixin
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.statefull.StatefulCrowdloanProviderFactory
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.validations.MainCrowdloanValidationSystem
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.validations.mainCrowdloan
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.main.CrowdloanViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorFactory

@Module(includes = [ViewModelModule::class])
class CrowdloanModule {

    @Provides
    @ScreenScope
    fun provideValidationSystem(): MainCrowdloanValidationSystem {
        return ValidationSystem.mainCrowdloan()
    }

    @Provides
    @ScreenScope
    fun provideCrowdloanMixinFactory(
        crowdloanSharedState: CrowdloanSharedState,
        crowdloanInteractor: CrowdloanInteractor,
        contributionsInteractor: ContributionsInteractor,
        selectedAccountUseCase: SelectedAccountUseCase,
        assetUseCase: AssetUseCase,
        amountFormatter: AmountFormatter
    ): StatefulCrowdloanMixin.Factory {
        return StatefulCrowdloanProviderFactory(
            singleAssetSharedState = crowdloanSharedState,
            crowdloanInteractor = crowdloanInteractor,
            contributionsInteractor = contributionsInteractor,
            selectedAccountUseCase = selectedAccountUseCase,
            assetUseCase = assetUseCase,
            amountFormatter = amountFormatter
        )
    }

    @Provides
    @IntoMap
    @ViewModelKey(CrowdloanViewModel::class)
    fun provideViewModel(
        resourceManager: ResourceManager,
        iconGenerator: AddressIconGenerator,
        crowdloanSharedState: CrowdloanSharedState,
        router: CrowdloanRouter,
        crowdloanUpdateSystem: UpdateSystem,
        assetSelectorFactory: AssetSelectorFactory,
        customDialogDisplayer: CustomDialogDisplayer.Presentation,
        customContributeManager: CustomContributeManager,
        statefulCrowdloanMixinFactory: StatefulCrowdloanMixin.Factory,
        validationExecutor: ValidationExecutor,
        validationSystem: MainCrowdloanValidationSystem,
    ): ViewModel {
        return CrowdloanViewModel(
            iconGenerator = iconGenerator,
            resourceManager = resourceManager,
            crowdloanSharedState = crowdloanSharedState,
            router = router,
            customContributeManager = customContributeManager,
            validationSystem = validationSystem,
            validationExecutor = validationExecutor,
            crowdloanUpdateSystem = crowdloanUpdateSystem,
            assetSelectorFactory = assetSelectorFactory,
            statefulCrowdloanMixinFactory = statefulCrowdloanMixinFactory,
            customDialogDisplayer = customDialogDisplayer
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): CrowdloanViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(CrowdloanViewModel::class.java)
    }
}
