package io.novafoundation.nova.feature_onboarding_impl.presentation.consentUpgrade.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.preferences.ConsentRepository
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_onboarding_impl.OnboardingRouter
import io.novafoundation.nova.feature_onboarding_impl.presentation.consentUpgrade.ConsentBannerUpgradeViewModel

@Module(includes = [ViewModelModule::class])
class ConsentBannerUpgradeModule {

    @Provides
    @IntoMap
    @ViewModelKey(ConsentBannerUpgradeViewModel::class)
    fun provideViewModel(
        router: OnboardingRouter,
        consentRepository: ConsentRepository,
        resourceManager: ResourceManager,
    ): ViewModel {
        return ConsentBannerUpgradeViewModel(
            router = router,
            consentRepository = consentRepository,
            resourceManager = resourceManager,
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ConsentBannerUpgradeViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ConsentBannerUpgradeViewModel::class.java)
    }
}
