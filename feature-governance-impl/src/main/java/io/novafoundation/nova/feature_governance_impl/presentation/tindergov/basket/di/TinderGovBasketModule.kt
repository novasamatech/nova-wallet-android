package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.basket.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_governance_api.domain.tindergov.TinderGovInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.VotersFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.basket.TinderGovBasketViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase

@Module(includes = [ViewModelModule::class])
class TinderGovBasketModule {

    @Provides
    @IntoMap
    @ViewModelKey(TinderGovBasketViewModel::class)
    fun provideViewModel(
        governanceSharedState: GovernanceSharedState,
        router: GovernanceRouter,
        interactor: TinderGovInteractor,
        votersFormatter: VotersFormatter,
        referendumFormatter: ReferendumFormatter,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        resourceManager: ResourceManager,
        assetUseCase: AssetUseCase
    ): ViewModel {
        return TinderGovBasketViewModel(
            governanceSharedState = governanceSharedState,
            router = router,
            interactor = interactor,
            votersFormatter = votersFormatter,
            referendumFormatter = referendumFormatter,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory,
            resourceManager = resourceManager,
            assetUseCase
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): TinderGovBasketViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(TinderGovBasketViewModel::class.java)
    }
}
