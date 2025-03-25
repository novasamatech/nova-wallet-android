package io.novafoundation.nova.feature_account_impl.presentation.legacyAddress.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.copyAddress.CopyAddressMixin
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.legacyAddress.ChainAddressSelectorPayload
import io.novafoundation.nova.feature_account_impl.presentation.legacyAddress.ChainAddressSelectorViewModel

@Module(includes = [ViewModelModule::class])
class ChainAddressSelectorModule {

    @Provides
    @IntoMap
    @ViewModelKey(ChainAddressSelectorViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        accountInteractor: AccountInteractor,
        payload: ChainAddressSelectorPayload,
        copyAddressMixin: CopyAddressMixin,
        appLinksProvider: AppLinksProvider
    ): ViewModel {
        return ChainAddressSelectorViewModel(
            router = router,
            payload = payload,
            accountInteractor = accountInteractor,
            copyAddressMixin = copyAddressMixin,
            appLinksProvider = appLinksProvider
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ChainAddressSelectorViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ChainAddressSelectorViewModel::class.java)
    }
}
