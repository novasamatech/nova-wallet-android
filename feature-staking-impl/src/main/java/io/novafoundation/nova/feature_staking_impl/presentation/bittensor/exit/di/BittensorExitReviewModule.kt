package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.di

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
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorEstimatesMixin
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorInteractor
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorSaleCostBasisFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitReviewPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitReviewViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.common.utils.formatting.duration.DurationFormatter
import javax.inject.Named

@Module(includes = [ViewModelModule::class])
class BittensorExitReviewModule {
    @Provides
    @IntoMap
    @ViewModelKey(BittensorExitReviewViewModel::class)
    fun review(
        analytics: AnalyticsService,
        operationAnalytics: BittensorOperationAnalytics,
        payload: BittensorExitReviewPayload,
        bittensorExitInteractor: BittensorExitInteractor,
        router: StakingRouter,
        resources: ResourceManager,
        assetIconProvider: AssetIconProvider,
        addressIconGenerator: AddressIconGenerator,
        tokenFormatter: TokenFormatter,
        fiatFormatter: FiatFormatter,
        chainRegistry: ChainRegistry,
        walletUiUseCase: WalletUiUseCase,
        accountRepository: AccountRepository,
        chainStateRepository: ChainStateRepository,
        externalActions: ExternalActions.Presentation,
        estimatesMixin: BittensorEstimatesMixin,
        retryController: BittensorRetryController,
        validationExecutor: ValidationExecutor,
        exitValidationSystem: BittensorExitValidationSystem,
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
        exitFormatter: BittensorExitFormatter,
        saleCostBasisFormatter: BittensorSaleCostBasisFormatter,
        validatorInteractor: BittensorValidatorInteractor,
        logger: Logger,
        @Named("BittensorWithdrawalDuration") durationFormatter: DurationFormatter
    ): ViewModel {
        return BittensorExitReviewViewModel(
            analytics, operationAnalytics, payload,
            bittensorExitInteractor,
            router,
            resources,
            assetIconProvider,
            addressIconGenerator,
            tokenFormatter,
            fiatFormatter,
            chainRegistry,
            walletUiUseCase,
            accountRepository,
            chainStateRepository,
            externalActions,
            estimatesMixin,
            retryController,
            validationExecutor,
            exitValidationSystem,
            feeLoaderMixinFactory,
            exitFormatter,
            saleCostBasisFormatter,
            validatorInteractor,
            logger,
            durationFormatter
        )
    }

    @Provides
    fun reviewVm(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorExitReviewViewModel =
        ViewModelProvider(fragment, factory)[BittensorExitReviewViewModel::class.java]
}
