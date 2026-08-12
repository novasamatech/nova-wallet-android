package io.novafoundation.nova.app.root.presentation.legal.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.app.root.presentation.legal.LegalConsentViewModel
import io.novafoundation.nova.common.data.legal.LegalConsentRepository
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule

@Module(
    includes = [
        ViewModelModule::class
    ]
)
class LegalConsentModule {

    @Provides
    @IntoMap
    @ViewModelKey(LegalConsentViewModel::class)
    fun provideViewModel(
        legalConsentRepository: LegalConsentRepository,
        appLinksProvider: AppLinksProvider
    ): ViewModel {
        return LegalConsentViewModel(
            legalConsentRepository,
            appLinksProvider
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): LegalConsentViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(LegalConsentViewModel::class.java)
    }
}
