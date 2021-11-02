package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.terms.di

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
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.moonbeam.MoonbeamCrowdloanInteractor
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.custom.moonbeam.MoonbeamTermsValidationSystem
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.terms.MoonbeamCrowdloanTermsViewModel
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.ContributePayload
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin

@Module(includes = [ViewModelModule::class])
class MoonbeamCrowdloanTermsModule {

    @Provides
    @IntoMap
    @ViewModelKey(MoonbeamCrowdloanTermsViewModel::class)
    fun provideViewModel(
        interactor: MoonbeamCrowdloanInteractor,
        payload: ContributePayload,
        feeLoaderMixin: FeeLoaderMixin.Presentation,
        resourceManager: ResourceManager,
        router: CrowdloanRouter,
        assetUseCase: AssetUseCase,
        validationSystem: MoonbeamTermsValidationSystem,
        validationExecutor: ValidationExecutor,
    ): ViewModel {
        return MoonbeamCrowdloanTermsViewModel(
            interactor,
            payload,
            feeLoaderMixin,
            resourceManager,
            router,
            assetUseCase,
            validationExecutor,
            validationSystem
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): MoonbeamCrowdloanTermsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(MoonbeamCrowdloanTermsViewModel::class.java)
    }
}
