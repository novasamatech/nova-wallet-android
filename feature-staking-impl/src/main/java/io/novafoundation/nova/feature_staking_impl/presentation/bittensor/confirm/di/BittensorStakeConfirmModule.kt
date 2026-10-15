package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.BittensorStakeConfirmViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class BittensorStakeConfirmModule {

    @Provides
    @IntoMap
    @ViewModelKey(BittensorStakeConfirmViewModel::class)
    fun provideViewModel(
        resourceManager: ResourceManager,
        router: StakingRouter,
        externalActions: ExternalActions.Presentation,
        chainRegistry: ChainRegistry,
        walletUiUseCase: WalletUiUseCase,
        selectedAccountUseCase: SelectedAccountUseCase,
        bittensorStakingInteractor: BittensorStakingInteractor,
        iconGenerator: AddressIconGenerator,
        fiatFormatter: FiatFormatter,
        tokenFormatter: TokenFormatter,
        bittensorAmountFormatter: BittensorAmountFormatter,
        retryController: BittensorRetryController
    ): ViewModel {
        return BittensorStakeConfirmViewModel(
            resourceManager = resourceManager,
            router = router,
            externalActions = externalActions,
            chainRegistry = chainRegistry,
            walletUiUseCase = walletUiUseCase,
            selectedAccountUseCase = selectedAccountUseCase,
            bittensorStakingInteractor = bittensorStakingInteractor,
            iconGenerator = iconGenerator,
            fiatFormatter = fiatFormatter,
            tokenFormatter = tokenFormatter,
            bittensorAmountFormatter = bittensorAmountFormatter,
            retryController = retryController
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): BittensorStakeConfirmViewModel {
        return ViewModelProvider(fragment, viewModelFactory)[BittensorStakeConfirmViewModel::class.java]
    }
}
