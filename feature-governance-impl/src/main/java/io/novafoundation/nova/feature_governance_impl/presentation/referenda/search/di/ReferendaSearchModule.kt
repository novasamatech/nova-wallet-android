package io.novafoundation.nova.feature_governance_impl.presentation.referenda.search.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_api.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.search.ReferendaSearchViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorFactory

@Module(includes = [ViewModelModule::class])
class ReferendaSearchModule {

    @Provides
    @IntoMap
    @ViewModelKey(ReferendaSearchViewModel::class)
    fun provideViewModel(
        assetSelectorFactory: AssetSelectorFactory,
        referendaListInteractor: ReferendaListInteractor,
        selectedAccountUseCase: SelectedAccountUseCase,
        selectedAssetSharedState: GovernanceSharedState,
        governanceRouter: GovernanceRouter,
        referendumFormatter: ReferendumFormatter,
        resourceManager: ResourceManager
    ): ViewModel {
        return ReferendaSearchViewModel(
            assetSelectorFactory = assetSelectorFactory,
            referendaListInteractor = referendaListInteractor,
            selectedAccountUseCase = selectedAccountUseCase,
            selectedAssetSharedState = selectedAssetSharedState,
            governanceRouter = governanceRouter,
            referendumFormatter = referendumFormatter,
            resourceManager = resourceManager
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ReferendaSearchViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ReferendaSearchViewModel::class.java)
    }
}
