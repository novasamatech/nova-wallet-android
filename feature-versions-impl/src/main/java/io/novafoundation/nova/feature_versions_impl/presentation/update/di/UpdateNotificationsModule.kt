package io.novafoundation.nova.feature_versions_impl.presentation.update.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.noties.markwon.Markwon
import io.novafoundation.nova.common.di.modules.shared.MarkdownFullModule
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor
import io.novafoundation.nova.feature_versions_api.presentation.VersionsRouter
import io.novafoundation.nova.feature_versions_impl.presentation.update.UpdateNotificationViewModel

@Module(includes = [ViewModelModule::class, MarkdownFullModule::class])
class UpdateNotificationsModule {

    @Provides
    @IntoMap
    @ViewModelKey(UpdateNotificationViewModel::class)
    fun provideViewModel(
        router: VersionsRouter,
        interactor: UpdateNotificationsInteractor,
        resourceManager: ResourceManager,
        markwon: Markwon,
    ): ViewModel {
        return UpdateNotificationViewModel(router, interactor, resourceManager, markwon)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): UpdateNotificationViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(UpdateNotificationViewModel::class.java)
    }
}
