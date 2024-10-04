package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.di

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.noties.markwon.Markwon
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.markdown.BoldStylePlugin
import io.novafoundation.nova.feature_governance_impl.domain.summary.ReferendaSummaryInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.TinderGovBasketInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.TinderGovInteractor
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov.TinderGovVoteCommunicator
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.TinderGovCardsViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase

@Module(includes = [ViewModelModule::class])
class TinderGovCardsModule {

    @Provides
    @ScreenScope
    fun provideMarkwon(context: Context): Markwon {
        return Markwon.builder(context)
            .usePlugin(BoldStylePlugin(context, R.font.public_sans_semi_bold, R.color.text_primary))
            .build()
    }

    @Provides
    @IntoMap
    @ViewModelKey(TinderGovCardsViewModel::class)
    fun provideViewModel(
        router: GovernanceRouter,
        tinderGovInteractor: TinderGovInteractor,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        tinderGovVoteCommunicator: TinderGovVoteCommunicator,
        resourceManager: ResourceManager,
        assetUseCase: AssetUseCase,
        referendaSummaryInteractor: ReferendaSummaryInteractor,
        tokenUseCase: TokenUseCase,
        basketInteractor: TinderGovBasketInteractor,
        markwon: Markwon
    ): ViewModel {
        return TinderGovCardsViewModel(
            router = router,
            interactor = tinderGovInteractor,
            basketInteractor = basketInteractor,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory,
            tinderGovVoteRequester = tinderGovVoteCommunicator,
            assetUseCase = assetUseCase,
            resourceManager = resourceManager,
            referendaSummaryInteractor = referendaSummaryInteractor,
            tokenUseCase = tokenUseCase,
            cardsMarkdown = markwon
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
