package io.novafoundation.nova.feature_account_impl.presentation.exporting.seed.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_impl.domain.account.export.seed.ExportSeedInteractor
import io.novafoundation.nova.feature_account_api.presenatation.AccountRouter
import io.novafoundation.nova.feature_account_api.presenatation.exporting.ExportPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.seed.ExportSeedViewModel

@Module(includes = [ViewModelModule::class])
class ExportSeedModule {

    @Provides
    @IntoMap
    @ViewModelKey(ExportSeedViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        interactor: ExportSeedInteractor,
        payload: ExportPayload,
    ): ViewModel {
        return ExportSeedViewModel(
            router,
            interactor,
            payload
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): ExportSeedViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ExportSeedViewModel::class.java)
    }
}
