package io.novafoundation.nova.feature_assets.presentation.manageTokens.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_assets.domain.manageTokens.ManageTokenInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.manageTokens.ManageTokensViewModel
import io.novafoundation.nova.feature_assets.presentation.manageTokens.model.MultiChainTokenMapper

@Module(includes = [ViewModelModule::class])
class ManageTokensModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): ManageTokensViewModel {
        return ViewModelProvider(fragment, factory).get(ManageTokensViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(ManageTokensViewModel::class)
    fun provideViewModel(
        router: AssetsRouter,
        interactor: ManageTokenInteractor,
        commonUiMapper: MultiChainTokenMapper
    ): ViewModel {
        return ManageTokensViewModel(
            router = router,
            interactor = interactor,
            commonUiMapper = commonUiMapper
        )
    }
}
