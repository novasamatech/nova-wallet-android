package io.novafoundation.nova.feature_assets.presentation.transaction.detail.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.transaction.detail.reward.pool.PoolRewardDetailViewModel
import io.novafoundation.nova.feature_staking_api.presentation.nominationPools.display.PoolDisplayUseCase
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class PoolRewardDetailModule {

    @Provides
    @IntoMap
    @ViewModelKey(PoolRewardDetailViewModel::class)
    fun provideViewModel(
        operation: OperationParcelizeModel.PoolReward,
        poolDisplayUseCase: PoolDisplayUseCase,
        router: AssetsRouter,
        chainRegistry: ChainRegistry,
        externalActions: ExternalActions.Presentation
    ): ViewModel {
        return PoolRewardDetailViewModel(
            operation = operation,
            poolDisplayUseCase = poolDisplayUseCase,
            router = router,
            chainRegistry = chainRegistry,
            externalActions = externalActions
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): PoolRewardDetailViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(PoolRewardDetailViewModel::class.java)
    }
}
