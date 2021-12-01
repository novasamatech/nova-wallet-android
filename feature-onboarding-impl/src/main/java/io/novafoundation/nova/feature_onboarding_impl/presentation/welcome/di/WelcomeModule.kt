package io.novafoundation.nova.feature_onboarding_impl.presentation.welcome.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.ImportTypeChooserMixin
import io.novafoundation.nova.feature_onboarding_impl.OnboardingRouter
import io.novafoundation.nova.feature_onboarding_impl.presentation.welcome.WelcomeViewModel

@Module(includes = [ViewModelModule::class])
class WelcomeModule {

    @Provides
    @IntoMap
    @ViewModelKey(WelcomeViewModel::class)
    fun provideViewModel(
        router: OnboardingRouter,
        appLinksProvider: AppLinksProvider,
        shouldShowBack: Boolean,
        importTypeChooserMixin: ImportTypeChooserMixin.Presentation,
        addAccountPayload: AddAccountPayload
    ): ViewModel {
        return WelcomeViewModel(shouldShowBack, router, appLinksProvider, addAccountPayload, importTypeChooserMixin)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): WelcomeViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(WelcomeViewModel::class.java)
    }
}
