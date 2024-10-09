package io.novafoundation.nova.feature_assets.presentation.novacard.overview.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_assets.BuildConfig
import io.novafoundation.nova.feature_assets.domain.novaCard.NovaCardInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.NovaCardViewModel
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.NovaCardWebViewControllerFactory
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.WebViewCardCreationInterceptorFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import okhttp3.OkHttpClient

@Module(includes = [ViewModelModule::class])
class NovaCardModule {

    @Provides
    fun provideWebViewCardCreationInterceptorFactory(
        gson: Gson,
        okHttpClient: OkHttpClient
    ): WebViewCardCreationInterceptorFactory = WebViewCardCreationInterceptorFactory(
        gson = gson,
        okHttpClient = okHttpClient
    )

    @Provides
    fun provideNovaCardWebViewControllerFactory(
        systemCallExecutor: SystemCallExecutor,
        permissionsAskerFactory: PermissionsAskerFactory,
        appLinksProvider: AppLinksProvider,
        fileProvider: FileProvider,
        gson: Gson,
        webViewCardCreationInterceptorFactory: WebViewCardCreationInterceptorFactory
    ): NovaCardWebViewControllerFactory {
        return NovaCardWebViewControllerFactory(
            systemCallExecutor,
            fileProvider,
            permissionsAskerFactory,
            appLinksProvider,
            gson,
            BuildConfig.NOVA_CARD_WIDGET_ID,
            webViewCardCreationInterceptorFactory
        )
    }

    @Provides
    @IntoMap
    @ViewModelKey(NovaCardViewModel::class)
    fun provideViewModel(
        chainRegistry: ChainRegistry,
        accountInteractor: AccountInteractor,
        assetsRouter: AssetsRouter,
        novaCardInteractor: NovaCardInteractor
    ): ViewModel {
        return NovaCardViewModel(
            chainRegistry = chainRegistry,
            accountInteractor = accountInteractor,
            assetsRouter = assetsRouter,
            novaCardInteractor = novaCardInteractor
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): NovaCardViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(NovaCardViewModel::class.java)
    }
}
