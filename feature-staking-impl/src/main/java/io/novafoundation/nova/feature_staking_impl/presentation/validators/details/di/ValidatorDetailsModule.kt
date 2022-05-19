package io.novafoundation.nova.feature_staking_impl.presentation.validators.details.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.ValidatorDetailsViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase

@Module(includes = [ViewModelModule::class])
class ValidatorDetailsModule {

    @Provides
    @IntoMap
    @ViewModelKey(ValidatorDetailsViewModel::class)
    fun provideViewModel(
        interactor: StakingInteractor,
        router: StakingRouter,
        payload: StakeTargetDetailsPayload,
        assetUseCase: AssetUseCase,
        addressIconGenerator: AddressIconGenerator,
        externalActions: ExternalActions.Presentation,
        appLinksProvider: AppLinksProvider,
        resourceManager: ResourceManager,
        singleAssetSharedState: StakingSharedState,
    ): ViewModel {
        return ValidatorDetailsViewModel(
            assetUseCase = assetUseCase,
            router = router,
            payload = payload,
            iconGenerator = addressIconGenerator,
            externalActions = externalActions,
            appLinksProvider = appLinksProvider,
            resourceManager = resourceManager,
            selectedAssetState = singleAssetSharedState
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ValidatorDetailsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ValidatorDetailsViewModel::class.java)
    }
}
