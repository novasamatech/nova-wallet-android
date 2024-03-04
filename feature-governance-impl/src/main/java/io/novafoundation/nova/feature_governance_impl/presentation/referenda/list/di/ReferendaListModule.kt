package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.dapp.GovernanceDAppsInteractor
import io.novafoundation.nova.feature_governance_impl.domain.filters.ReferendaFiltersInteractor
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.ReferendaListViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorFactory

@Module(includes = [ViewModelModule::class])
class ReferendaListModule {

    @Provides
    @IntoMap
    @ViewModelKey(ReferendaListViewModel::class)
    fun provideViewModel(
        assetSelectorFactory: AssetSelectorFactory,
        referendaListInteractor: ReferendaListInteractor,
        referendaFiltersInteractor: ReferendaFiltersInteractor,
        selectedAccountUseCase: SelectedAccountUseCase,
        selectedAssetSharedState: GovernanceSharedState,
        resourceManager: ResourceManager,
        updateSystem: UpdateSystem,
        governanceRouter: GovernanceRouter,
        referendumFormatter: ReferendumFormatter,
        governanceDAppsInteractor: GovernanceDAppsInteractor
    ): ViewModel {
        return ReferendaListViewModel(
            assetSelectorFactory = assetSelectorFactory,
            referendaListInteractor = referendaListInteractor,
            referendaFiltersInteractor = referendaFiltersInteractor,
            selectedAccountUseCase = selectedAccountUseCase,
            selectedAssetSharedState = selectedAssetSharedState,
            resourceManager = resourceManager,
            updateSystem = updateSystem,
            governanceRouter = governanceRouter,
            referendumFormatter = referendumFormatter,
            governanceDAppsInteractor = governanceDAppsInteractor
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ReferendaListViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ReferendaListViewModel::class.java)
    }
}
