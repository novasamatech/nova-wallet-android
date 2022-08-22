package io.novafoundation.nova.feature_assets.presentation.transaction.filter.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.transaction.filter.HistoryFiltersProviderFactory
import io.novafoundation.nova.feature_assets.presentation.transaction.filter.TransactionHistoryFilterViewModel

@Module(includes = [ViewModelModule::class])
class TransactionHistoryFilterModule {

    @Provides
    @IntoMap
    @ViewModelKey(TransactionHistoryFilterViewModel::class)
    fun provideViewModel(
        router: AssetsRouter,
        historyFiltersProviderFactory: HistoryFiltersProviderFactory
    ): ViewModel {
        return TransactionHistoryFilterViewModel(router, historyFiltersProviderFactory)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): TransactionHistoryFilterViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(TransactionHistoryFilterViewModel::class.java)
    }
}
