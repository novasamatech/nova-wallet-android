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
import io.novafoundation.nova.feature_governance_api.domain.tindergov.TinderGovInteractor
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.TinderGovCardDetailsLoader
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.TinderGovCardsViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase

@Module(includes = [ViewModelModule::class])
class TinderGovCardsModule {

    @Provides
    fun provideTinderGovCardsDataHelper(
        interactor: TinderGovInteractor,
        assetUseCase: AssetUseCase,
    ): TinderGovCardDetailsLoader {
        return TinderGovCardDetailsLoader(
            interactor,
            assetUseCase
        )
    }

    @Provides
    @IntoMap
    @ViewModelKey(TinderGovCardsViewModel::class)
    fun provideViewModel(
        router: GovernanceRouter,
        tinderGovInteractor: TinderGovInteractor,
        tinderGovCardDetailsLoader: TinderGovCardDetailsLoader,
        referendumFormatter: ReferendumFormatter,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory
    ): ViewModel {
        return TinderGovCardsViewModel(
            router,
            tinderGovCardDetailsLoader,
            tinderGovInteractor,
            referendumFormatter,
            actionAwaitableMixinFactory
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
