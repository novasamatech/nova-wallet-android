package io.novafoundation.nova.feature_assets.presentation.transaction.detail.di

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
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.transaction.detail.extrinsic.ExtrinsicDetailViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class ExtrinsicDetailModule {
    @Provides
    @IntoMap
    @ViewModelKey(ExtrinsicDetailViewModel::class)
    fun provideViewModel(
        addressDisplayUseCase: AddressDisplayUseCase,
        addressIconGenerator: AddressIconGenerator,
        chainRegistry: ChainRegistry,
        router: AssetsRouter,
        operation: OperationParcelizeModel.Extrinsic,
        externalActions: ExternalActions.Presentation,
        resourceManager: ResourceManager,
        assetIconProvider: AssetIconProvider
    ): ViewModel {
        return ExtrinsicDetailViewModel(
            addressDisplayUseCase,
            addressIconGenerator,
            chainRegistry,
            router,
            operation,
            externalActions,
            resourceManager,
            assetIconProvider
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ExtrinsicDetailViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ExtrinsicDetailViewModel::class.java)
    }
}
