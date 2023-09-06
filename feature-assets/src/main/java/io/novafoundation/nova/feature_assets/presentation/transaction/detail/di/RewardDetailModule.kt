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
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.transaction.detail.reward.direct.RewardDetailViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class RewardDetailModule {

    @Provides
    @IntoMap
    @ViewModelKey(RewardDetailViewModel::class)
    fun provideViewModel(
        operation: OperationParcelizeModel.Reward,
        addressIconGenerator: AddressIconGenerator,
        addressDisplayUseCase: AddressDisplayUseCase,
        chainRegistry: ChainRegistry,
        externalActions: ExternalActions.Presentation,
        router: AssetsRouter
    ): ViewModel {
        return RewardDetailViewModel(
            operation,
            addressIconGenerator,
            addressDisplayUseCase,
            router,
            chainRegistry,
            externalActions,
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): RewardDetailViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(RewardDetailViewModel::class.java)
    }
}
