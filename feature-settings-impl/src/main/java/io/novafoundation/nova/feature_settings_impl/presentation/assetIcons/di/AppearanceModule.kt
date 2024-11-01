package io.novafoundation.nova.feature_settings_impl.presentation.assetIcons.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.AppearanceInteractor
import io.novafoundation.nova.feature_settings_impl.presentation.assetIcons.AppearanceViewModel

@Module(includes = [ViewModelModule::class])
class AppearanceModule {

    @Provides
    @IntoMap
    @ViewModelKey(AppearanceViewModel::class)
    fun provideViewModel(
        interactor: AppearanceInteractor,
        router: SettingsRouter
    ): ViewModel {
        return AppearanceViewModel(
            interactor,
            router
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): AppearanceViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(AppearanceViewModel::class.java)
    }
}
