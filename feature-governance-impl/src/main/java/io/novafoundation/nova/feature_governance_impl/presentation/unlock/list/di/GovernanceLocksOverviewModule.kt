package io.novafoundation.nova.feature_governance_impl.presentation.unlock.list.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.GovernanceUnlockInteractor
import io.novafoundation.nova.feature_governance_api.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.unlock.list.GovernanceLocksOverviewViewModel
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase

@Module(includes = [ViewModelModule::class])
class GovernanceLocksOverviewModule {

    @Provides
    @IntoMap
    @ViewModelKey(GovernanceLocksOverviewViewModel::class)
    fun provideViewModel(
        router: GovernanceRouter,
        interactor: GovernanceUnlockInteractor,
        tokenUseCase: TokenUseCase,
        resourceManager: ResourceManager,
    ): ViewModel {
        return GovernanceLocksOverviewViewModel(
            router = router,
            interactor = interactor,
            tokenUseCase = tokenUseCase,
            resourceManager = resourceManager
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): GovernanceLocksOverviewViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(GovernanceLocksOverviewViewModel::class.java)
    }
}
