package io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_crowdloan_impl.data.CrowdloanSharedState
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.CrowdloanInteractor
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions.UserContributionsViewModel

@Module(includes = [ViewModelModule::class])
class UserContributionsModule {

    @Provides
    @IntoMap
    @ViewModelKey(UserContributionsViewModel::class)
    fun provideViewModel(
        interactor: CrowdloanInteractor,
        iconGenerator: AddressIconGenerator,
        crowdloanSharedState: CrowdloanSharedState,
        router: CrowdloanRouter,
    ): ViewModel {
        return UserContributionsViewModel(
            interactor,
            iconGenerator,
            router,
            crowdloanSharedState
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): UserContributionsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(UserContributionsViewModel::class.java)
    }
}
