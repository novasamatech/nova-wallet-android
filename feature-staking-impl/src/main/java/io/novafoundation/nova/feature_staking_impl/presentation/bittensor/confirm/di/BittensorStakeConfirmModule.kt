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
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.BittensorStakeConfirmPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.BittensorStakeConfirmViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class BittensorStakeConfirmModule {

    @Provides
    @IntoMap
    @ViewModelKey(BittensorStakeConfirmViewModel::class)
    fun provideViewModel(
        payload: BittensorStakeConfirmPayload,
        resourceManager: ResourceManager,
        router: StakingRouter,
        externalActions: ExternalActions.Presentation,
        chainRegistry: ChainRegistry,
        walletUiUseCase: WalletUiUseCase,
        accountRepository: AccountRepository,
        bittensorStakingInteractor: BittensorStakingInteractor,
        iconGenerator: AddressIconGenerator,
        fiatFormatter: FiatFormatter,
        tokenFormatter: TokenFormatter,
        bittensorAmountFormatter: BittensorAmountFormatter,
        retryController: BittensorRetryController,
        validationSystem: BittensorStakePreflightSystem,
        validationExecutor: ValidationExecutor,
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory
    ): ViewModel {
        return BittensorStakeConfirmViewModel(
            payload = payload,
            resourceManager = resourceManager,
            router = router,
            externalActions = externalActions,
            chainRegistry = chainRegistry,
            walletUiUseCase = walletUiUseCase,
            accountRepository = accountRepository,
            bittensorStakingInteractor = bittensorStakingInteractor,
            iconGenerator = iconGenerator,
            fiatFormatter = fiatFormatter,
            tokenFormatter = tokenFormatter,
            bittensorAmountFormatter = bittensorAmountFormatter,
            retryController = retryController,
            validationSystem = validationSystem,
            validationExecutor = validationExecutor,
            feeLoaderMixinFactory = feeLoaderMixinFactory
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
