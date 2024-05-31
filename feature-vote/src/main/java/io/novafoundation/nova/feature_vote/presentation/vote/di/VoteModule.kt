package io.novafoundation.nova.feature_vote.presentation.vote.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_vote.presentation.VoteRouter
import io.novafoundation.nova.feature_vote.presentation.vote.VoteViewModel

@Module(includes = [ViewModelModule::class])
class VoteModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): VoteViewModel {
        return ViewModelProvider(fragment, factory).get(VoteViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(VoteViewModel::class)
    fun provideViewModel(
        voteRouter: VoteRouter,
        selectedAccountUseCase: SelectedAccountUseCase
    ): ViewModel {
        return VoteViewModel(
            router = voteRouter,
            selectedAccountUseCase = selectedAccountUseCase
        )
    }
}
