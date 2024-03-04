package io.novafoundation.nova.feature_account_impl.presentation.exporting.json.confirm.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.ExportJsonConfirmPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.confirm.ExportJsonConfirmViewModel

@Module(includes = [ViewModelModule::class])
class ExportJsonConfirmModule {

    @Provides
    @IntoMap
    @ViewModelKey(ExportJsonConfirmViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        payload: ExportJsonConfirmPayload
    ): ViewModel {
        return ExportJsonConfirmViewModel(
            router,
            payload
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): ExportJsonConfirmViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ExportJsonConfirmViewModel::class.java)
    }
}
