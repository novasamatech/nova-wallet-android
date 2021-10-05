package jp.co.soramitsu.feature_account_impl.presentation.account.create.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import jp.co.soramitsu.common.di.viewmodel.ViewModelKey
import jp.co.soramitsu.common.di.viewmodel.ViewModelModule
import jp.co.soramitsu.common.mixin.MixinFactory
import jp.co.soramitsu.core.model.Node
import jp.co.soramitsu.feature_account_api.domain.interfaces.AccountInteractor
import jp.co.soramitsu.feature_account_api.presenatation.account.add.AddAccountPayload
import jp.co.soramitsu.feature_account_impl.presentation.AccountRouter
import jp.co.soramitsu.feature_account_impl.presentation.account.create.CreateAccountViewModel
import jp.co.soramitsu.feature_account_impl.presentation.common.mixin.api.ForcedChainMixin
import jp.co.soramitsu.feature_account_impl.presentation.common.mixin.impl.ForcedChainMixinFactory
import jp.co.soramitsu.feature_account_impl.presentation.common.mixin.impl.ForcedChainProvider
import jp.co.soramitsu.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class CreateAccountModule {

    @Provides
    fun provideForcedChainMixinFactory(
        chainRegistry: ChainRegistry,
        payload: AddAccountPayload
    ): MixinFactory<ForcedChainMixin> {
        return ForcedChainMixinFactory(chainRegistry, payload)
    }

    @Provides
    @IntoMap
    @ViewModelKey(CreateAccountViewModel::class)
    fun provideViewModel(
        router: AccountRouter,
        payload: AddAccountPayload,
        forcedChainMixinFactory: MixinFactory<ForcedChainMixin>
    ): ViewModel {
        return CreateAccountViewModel(router, payload, forcedChainMixinFactory)
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, viewModelFactory: ViewModelProvider.Factory): CreateAccountViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(CreateAccountViewModel::class.java)
    }
}
