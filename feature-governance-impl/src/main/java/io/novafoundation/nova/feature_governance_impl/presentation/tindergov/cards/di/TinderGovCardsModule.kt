package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.di

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
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov.TinderGovVoteCommunicator
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.TinderGovCardsDetailsLoaderFactory
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.TinderGovCardsViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase

@Module(includes = [ViewModelModule::class])
class TinderGovCardsModule {

    @Provides
    fun provideTinderGovCardsDataHelper(
        interactor: TinderGovInteractor,
        tokenUseCase: TokenUseCase,
    ): TinderGovCardsDetailsLoaderFactory {
        return TinderGovCardsDetailsLoaderFactory(
            interactor,
            tokenUseCase
        )
    }

    @Provides
    @IntoMap
    @ViewModelKey(TinderGovCardsViewModel::class)
    fun provideViewModel(
        router: GovernanceRouter,
        tinderGovInteractor: TinderGovInteractor,
        tinderGovCardDetailsLoaderFactory: TinderGovCardsDetailsLoaderFactory,
        referendumFormatter: ReferendumFormatter,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        tinderGovVoteCommunicator: TinderGovVoteCommunicator,
        resourceManager: ResourceManager,
        assetUseCase: AssetUseCase
    ): ViewModel {
        return TinderGovCardsViewModel(
            router,
            tinderGovCardDetailsLoaderFactory,
            tinderGovInteractor,
            referendumFormatter,
            actionAwaitableMixinFactory,
            tinderGovVoteCommunicator,
            assetUseCase,
            resourceManager
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): TinderGovCardsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(TinderGovCardsViewModel::class.java)
    }
}
