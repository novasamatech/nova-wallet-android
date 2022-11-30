package io.novafoundation.nova.feature_assets.presentation.tokens.add.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_assets.domain.tokens.add.AddTokensInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.tokens.add.selectChain.AddTokenSelectChainViewModel

@Module(includes = [ViewModelModule::class])
class AddTokenSelectChainModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): AddTokenSelectChainViewModel {
        return ViewModelProvider(fragment, factory).get(AddTokenSelectChainViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(AddTokenSelectChainViewModel::class)
    fun provideViewModel(
        router: AssetsRouter,
        interactor: AddTokensInteractor,
    ): ViewModel {
        return AddTokenSelectChainViewModel(
            router = router,
            interactor = interactor,
        )
    }
}
