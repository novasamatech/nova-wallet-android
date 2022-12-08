package io.novafoundation.nova.feature_assets.presentation.tokens.manage.chain.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_assets.domain.tokens.manage.ManageTokenInteractor
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.chain.ManageChainTokensPayload
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.chain.ManageChainTokensViewModel
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.model.MultiChainTokenMapper

@Module(includes = [ViewModelModule::class])
class ManageChainTokensModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): ManageChainTokensViewModel {
        return ViewModelProvider(fragment, factory).get(ManageChainTokensViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(ManageChainTokensViewModel::class)
    fun provideViewModel(
        interactor: ManageTokenInteractor,
        uiMapper: MultiChainTokenMapper,
        payload: ManageChainTokensPayload,
    ): ViewModel {
        return ManageChainTokensViewModel(
            interactor = interactor,
            commonUiMapper = uiMapper,
            payload = payload,
        )
    }
}
