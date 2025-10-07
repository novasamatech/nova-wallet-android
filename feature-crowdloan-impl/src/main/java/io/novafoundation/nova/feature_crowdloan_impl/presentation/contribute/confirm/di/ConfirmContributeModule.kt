package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.confirm.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_crowdloan_impl.data.CrowdloanSharedState
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeManager
import io.novafoundation.nova.feature_crowdloan_impl.di.validations.Confirm
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.CrowdloanContributeInteractor
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.ContributeValidation
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.confirm.ConfirmContributeViewModel
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.confirm.parcel.ConfirmContributePayload
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter

@Module(includes = [ViewModelModule::class])
class ConfirmContributeModule {

    @Provides
    @IntoMap
    @ViewModelKey(ConfirmContributeViewModel::class)
    fun provideViewModel(
        assetIconProvider: AssetIconProvider,
        interactor: CrowdloanContributeInteractor,
        router: CrowdloanRouter,
        resourceManager: ResourceManager,
        assetUseCase: AssetUseCase,
        validationExecutor: ValidationExecutor,
        payload: ConfirmContributePayload,
        accountUseCase: SelectedAccountUseCase,
        addressIconGenerator: AddressIconGenerator,
        @Confirm contributeValidations: @JvmSuppressWildcards Set<ContributeValidation>,
        externalActions: ExternalActions.Presentation,
        customContributeManager: CustomContributeManager,
        singleAssetSharedState: CrowdloanSharedState,
        extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
        amountFormatter: AmountFormatter
    ): ViewModel {
        return ConfirmContributeViewModel(
            assetIconProvider,
            router,
            interactor,
            resourceManager,
            assetUseCase,
            accountUseCase,
            addressIconGenerator,
            validationExecutor,
            payload,
            contributeValidations,
            customContributeManager,
            externalActions,
            singleAssetSharedState,
            extrinsicNavigationWrapper,
            amountFormatter = amountFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ConfirmContributeViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ConfirmContributeViewModel::class.java)
    }
}
