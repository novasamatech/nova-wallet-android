package io.novafoundation.nova.feature_assets.presentation.tokens.manage.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.repository.AssetsViewModeRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.AutoEnableTokensRepository
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_assets.domain.tokens.manage.ManageTokenInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.ManageTokensViewModel
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.model.ManageAssetsMapper

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
        mapper: ManageAssetsMapper,
        autoEnableTokensRepository: AutoEnableTokensRepository,
        accountRepository: AccountRepository,
        assetsViewModeRepository: AssetsViewModeRepository,
    ): ViewModel {
        return ManageTokensViewModel(
            router = router,
            interactor = interactor,
            mapper = mapper,
            autoEnableTokensRepository = autoEnableTokensRepository,
            accountRepository = accountRepository,
            assetsViewModeRepository = assetsViewModeRepository
        )
    }
}
