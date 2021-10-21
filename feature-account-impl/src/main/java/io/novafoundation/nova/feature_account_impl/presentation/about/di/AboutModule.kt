package io.novafoundation.nova.feature_account_impl.presentation.about.di

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.about.AboutViewModel

@Module(includes = [ViewModelModule::class])
class AboutModule {

    @Provides
    @IntoMap
    @ViewModelKey(AboutViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        context: Context,
        appLinksProvider: AppLinksProvider,
        resourceManager: ResourceManager
    ): ViewModel {
        return AboutViewModel(router, context, appLinksProvider, resourceManager)
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): AboutViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(AboutViewModel::class.java)
    }
}
