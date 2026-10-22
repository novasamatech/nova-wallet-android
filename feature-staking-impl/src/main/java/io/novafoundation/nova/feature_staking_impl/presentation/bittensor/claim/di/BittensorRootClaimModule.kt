package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.claim.di

import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorOperationAnalytics

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
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.utils.formatting.baseDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.DurationFormatter
import javax.inject.Named
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
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootValidatorSource
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter

@Module(includes = [ViewModelModule::class])
class BittensorRootClaimModule {

    @Provides
    @Named("BittensorClaimDuration")
    fun durationFormatter(contextManager: ContextManager): DurationFormatter =
        baseDurationFormatter(contextManager.getApplicationContext())

    @Provides
    @IntoMap
    @ViewModelKey(BittensorRootClaimViewModel::class)
    fun claim(
        analytics: AnalyticsService,
        operationAnalytics: BittensorOperationAnalytics,
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
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
        @Named("BittensorClaimDuration") durationFormatter: DurationFormatter,
        amountFormatter: BittensorAmountFormatter,
        rootValidatorSource: BittensorRootValidatorSource,
        debugSettings: io.novafoundation.nova.common.data.storage.DebugSettings
    ): ViewModel =
        BittensorRootClaimViewModel(
            analytics, operationAnalytics,
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
            feeLoaderMixinFactory,
            durationFormatter,
            amountFormatter,
            rootValidatorSource,
            debugSettings
        )

    @Provides
    fun claimVm(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorRootClaimViewModel =
        ViewModelProvider(fragment, factory)[BittensorRootClaimViewModel::class.java]
}
