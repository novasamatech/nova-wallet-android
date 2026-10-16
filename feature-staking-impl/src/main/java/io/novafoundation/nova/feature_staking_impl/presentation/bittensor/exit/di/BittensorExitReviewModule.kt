package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.di

import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorEstimatesRepository
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicSubmissionRecovery
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitReviewViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitReviewPayload

@Module(includes = [ViewModelModule::class])
class BittensorExitReviewModule {
    @Provides
    @IntoMap
    @ViewModelKey(BittensorExitReviewViewModel::class)
    fun review(
        payload: BittensorExitReviewPayload,
        interactor: BittensorExitInteractor,
        router: StakingRouter,
        resources: ResourceManager,
        icons: AssetIconProvider,
        addresses: AddressIconGenerator,
        tokens: TokenFormatter,
        fiatFormatter: FiatFormatter,
        recovery: ExtrinsicSubmissionRecovery,
        chains: ChainRegistry,
        walletUi: WalletUiUseCase,
        accounts: SelectedAccountUseCase,
        chainState: ChainStateRepository,
        externalActions: ExternalActions.Presentation,
        estimates: BittensorEstimatesRepository,
        logger: Logger
    ): ViewModel {
        return BittensorExitReviewViewModel(
            payload, interactor, router, resources, icons, addresses, tokens, fiatFormatter, recovery,
            chains, walletUi, accounts, chainState, externalActions, estimates, logger
        )
    }

    @Provides
    fun reviewVm(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorExitReviewViewModel =
        ViewModelProvider(fragment, factory)[BittensorExitReviewViewModel::class.java]
}
