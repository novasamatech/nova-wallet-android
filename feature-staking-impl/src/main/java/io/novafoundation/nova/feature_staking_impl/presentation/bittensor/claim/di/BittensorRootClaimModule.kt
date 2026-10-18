package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.claim.di

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
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorClaimValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootClaimInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.claim.BittensorRootClaimPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.claim.BittensorRootClaimViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger

@Module(includes = [ViewModelModule::class])
class BittensorRootClaimModule {
    @Provides
    @IntoMap
    @ViewModelKey(BittensorRootClaimViewModel::class)
    fun claim(
        payload: BittensorRootClaimPayload,
        bittensorRootClaimInteractor: BittensorRootClaimInteractor,
        router: StakingRouter,
        resources: ResourceManager,
        tokenFormatter: TokenFormatter,
        addressIconGenerator: AddressIconGenerator,
        externalActions: ExternalActions.Presentation,
        logger: Logger,
        walletUiUseCase: WalletUiUseCase,
        retryController: BittensorRetryController,
        validationExecutor: ValidationExecutor,
        claimValidationSystem: BittensorClaimValidationSystem,
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory
    ): ViewModel =
        BittensorRootClaimViewModel(
            payload,
            bittensorRootClaimInteractor,
            router,
            resources,
            tokenFormatter,
            addressIconGenerator,
            externalActions,
            logger,
            walletUiUseCase,
            retryController,
            validationExecutor,
            claimValidationSystem,
            feeLoaderMixinFactory
        )

    @Provides
    fun claimVm(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorRootClaimViewModel =
        ViewModelProvider(fragment, factory)[BittensorRootClaimViewModel::class.java]
}
