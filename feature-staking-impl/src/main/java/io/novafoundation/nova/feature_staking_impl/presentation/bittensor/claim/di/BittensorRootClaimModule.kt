package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.claim.di

import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootClaimInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.claim.BittensorRootClaimViewModel
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicSubmissionRecovery
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter

@Module(includes = [ViewModelModule::class])
class BittensorRootClaimModule {
    @Provides
    @IntoMap
    @ViewModelKey(BittensorRootClaimViewModel::class)
    fun claim(
        interactor: BittensorRootClaimInteractor,
        router: StakingRouter,
        resources: ResourceManager,
        tokens: TokenFormatter,
        addresses: AddressIconGenerator,
        recovery: ExtrinsicSubmissionRecovery,
        externalActions: ExternalActions.Presentation,
        logger: Logger,
        walletUi: WalletUiUseCase
    ): ViewModel =
        BittensorRootClaimViewModel(interactor, router, resources, tokens, addresses, recovery, externalActions, logger, walletUi)

    @Provides
    fun claimVm(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorRootClaimViewModel =
        ViewModelProvider(fragment, factory)[BittensorRootClaimViewModel::class.java]
}
