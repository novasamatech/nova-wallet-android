package jp.co.soramitsu.feature_wallet_impl.presentation.transaction.detail.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import jp.co.soramitsu.common.address.AddressIconGenerator
import jp.co.soramitsu.common.di.viewmodel.ViewModelKey
import jp.co.soramitsu.common.di.viewmodel.ViewModelModule
import jp.co.soramitsu.feature_account_api.presenatation.account.AddressDisplayUseCase
import jp.co.soramitsu.feature_account_api.presenatation.actions.ExternalActions
import jp.co.soramitsu.feature_wallet_impl.presentation.WalletRouter
import jp.co.soramitsu.feature_wallet_impl.presentation.model.OperationParcelizeModel
import jp.co.soramitsu.feature_wallet_impl.presentation.transaction.detail.extrinsic.ExtrinsicDetailViewModel
import jp.co.soramitsu.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class ExtrinsicDetailModule {
    @Provides
    @IntoMap
    @ViewModelKey(ExtrinsicDetailViewModel::class)
    fun provideViewModel(
        addressDisplayUseCase: AddressDisplayUseCase,
        addressIconGenerator: AddressIconGenerator,
        chainRegistry: ChainRegistry,
        router: WalletRouter,
        operation: OperationParcelizeModel.Extrinsic,
        externalActions: ExternalActions.Presentation,
    ): ViewModel {
        return ExtrinsicDetailViewModel(
            addressDisplayUseCase,
            addressIconGenerator,
            chainRegistry,
            router,
            operation,
            externalActions,
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ExtrinsicDetailViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ExtrinsicDetailViewModel::class.java)
    }
}
