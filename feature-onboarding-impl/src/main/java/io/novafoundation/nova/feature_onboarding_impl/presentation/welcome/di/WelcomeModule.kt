package io.novafoundation.nova.feature_onboarding_impl.presentation.welcome.di

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
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_onboarding_impl.OnboardingRouter
import io.novafoundation.nova.feature_onboarding_impl.presentation.welcome.WelcomeViewModel
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor

@Module(includes = [ViewModelModule::class])
class WelcomeModule {

    @Provides
    @IntoMap
    @ViewModelKey(WelcomeViewModel::class)
    fun provideViewModel(
        router: OnboardingRouter,
        shouldShowBack: Boolean,
        addAccountPayload: AddAccountPayload,
        updateNotificationsInteractor: UpdateNotificationsInteractor,
        consentRepository: ConsentRepository,
        resourceManager: ResourceManager,
    ): ViewModel {
        return WelcomeViewModel(
            shouldShowBack = shouldShowBack,
            router = router,
            addAccountPayload = addAccountPayload,
            updateNotificationsInteractor = updateNotificationsInteractor,
            consentRepository = consentRepository,
            resourceManager = resourceManager,
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): WelcomeViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(WelcomeViewModel::class.java)
    }
}
