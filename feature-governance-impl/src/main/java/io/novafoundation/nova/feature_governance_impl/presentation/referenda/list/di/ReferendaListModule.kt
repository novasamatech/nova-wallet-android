package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.ReferendaListViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorMixin

@Module(includes = [ViewModelModule::class])
class ReferendaListModule {

    @Provides
    @IntoMap
    @ViewModelKey(ReferendaListViewModel::class)
    fun provideViewModel(
        assetSelectorFactory: MixinFactory<AssetSelectorMixin.Presentation>,
    ): ViewModel {
        return ReferendaListViewModel(assetSelectorFactory)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ReferendaListViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ReferendaListViewModel::class.java)
    }
}
