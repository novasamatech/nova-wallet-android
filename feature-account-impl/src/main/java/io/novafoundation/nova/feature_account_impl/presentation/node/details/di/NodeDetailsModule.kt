package io.novafoundation.nova.feature_account_impl.presentation.node.details.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.node.details.NodeDetailsViewModel

@Module(includes = [ViewModelModule::class])
class NodeDetailsModule {

    @Provides
    @IntoMap
    @ViewModelKey(NodeDetailsViewModel::class)
    fun provideViewModel(
        interactor: AccountInteractor,
        router: AccountRouter,
        nodeId: Int,
        clipboardManager: ClipboardManager,
        resourceManager: ResourceManager
    ): ViewModel {
        return NodeDetailsViewModel(interactor, router, nodeId, clipboardManager, resourceManager)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): NodeDetailsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(NodeDetailsViewModel::class.java)
    }
}
