package io.novafoundation.nova.feature_assets.presentation.tokens.add.enterInfo.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_assets.domain.tokens.add.AddTokensInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.tokens.add.enterInfo.AddTokenEnterInfoPayload
import io.novafoundation.nova.feature_assets.presentation.tokens.add.enterInfo.AddTokenEnterInfoViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class AddTokenEnterInfoModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): AddTokenEnterInfoViewModel {
        return ViewModelProvider(fragment, factory).get(AddTokenEnterInfoViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(AddTokenEnterInfoViewModel::class)
    fun provideViewModel(
        router: AssetsRouter,
        interactor: AddTokensInteractor,
        resourceManager: ResourceManager,
        payload: AddTokenEnterInfoPayload,
        validationExecutor: ValidationExecutor,
        chainRegistry: ChainRegistry
    ): ViewModel {
        return AddTokenEnterInfoViewModel(
            router = router,
            interactor = interactor,
            resourceManager = resourceManager,
            payload = payload,
            validationExecutor = validationExecutor,
            chainRegistry = chainRegistry,
        )
    }
}
