package io.novafoundation.nova.feature_assets.presentation.receive.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.repository.AssetsIconModeRepository
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.QrCodeGenerator
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.domain.receive.ReceiveInteractor
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.receive.ReceiveViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class ReceiveModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        fileProvider: FileProvider,
        chainRegistry: ChainRegistry,
        accountRepository: AccountRepository,
        assetsIconModeRepository: AssetsIconModeRepository
    ) = ReceiveInteractor(fileProvider, chainRegistry, accountRepository, assetsIconModeRepository)

    @Provides
    @IntoMap
    @ViewModelKey(ReceiveViewModel::class)
    fun provideViewModel(
        interactor: ReceiveInteractor,
        qrCodeGenerator: QrCodeGenerator,
        resourceManager: ResourceManager,
        router: AssetsRouter,
        chainRegistry: ChainRegistry,
        selectedAccountUseCase: SelectedAccountUseCase,
        payload: AssetPayload,
        clipboardManager: ClipboardManager
    ): ViewModel {
        return ReceiveViewModel(
            interactor,
            qrCodeGenerator,
            resourceManager,
            payload,
            chainRegistry,
            selectedAccountUseCase,
            router,
            clipboardManager
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ReceiveViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ReceiveViewModel::class.java)
    }
}
