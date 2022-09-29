package io.novafoundation.nova.feature_vote.presentation.vote.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_vote.presentation.VoteRouter
import io.novafoundation.nova.feature_vote.presentation.vote.VoteViewModel

@Module(includes = [ViewModelModule::class])
class VoteModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): VoteViewModel {
        return ViewModelProvider(fragment, factory).get(VoteViewModel::class.java)
    }

    @Provides
    @ScreenScope
    fun provideVoteRouter(
        routerFactory: VoteRouter.Factory,
        fragment: Fragment
    ): VoteRouter {
        return routerFactory.create(fragment)
    }

    @Provides
    @IntoMap
    @ViewModelKey(VoteViewModel::class)
    fun provideViewModel(
        voteRouter: VoteRouter
    ): ViewModel {
        return VoteViewModel(
            voteRouter = voteRouter
        )
    }
}
