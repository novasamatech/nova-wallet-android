package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeManager
import io.novafoundation.nova.feature_crowdloan_impl.di.validations.Select
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.CrowdloanContributeInteractor
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.ContributeValidation
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.CrowdloanContributeViewModel
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.ContributePayload
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class CrowdloanContributeModule {

    @Provides
    @IntoMap
    @ViewModelKey(CrowdloanContributeViewModel::class)
    fun provideViewModel(
        assetIconProvider: AssetIconProvider,
        interactor: CrowdloanContributeInteractor,
        router: CrowdloanRouter,
        resourceManager: ResourceManager,
        assetUseCase: AssetUseCase,
        validationExecutor: ValidationExecutor,
        feeLoaderMixin: FeeLoaderMixin.Presentation,
        payload: ContributePayload,
        @Select contributeValidations: @JvmSuppressWildcards Set<ContributeValidation>,
        customContributeManager: CustomContributeManager,
    ): ViewModel {
        return CrowdloanContributeViewModel(
            assetIconProvider,
            router,
            interactor,
            resourceManager,
            assetUseCase,
            validationExecutor,
            feeLoaderMixin,
            payload,
            contributeValidations,
            customContributeManager
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): CrowdloanContributeViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(CrowdloanContributeViewModel::class.java)
    }
}
