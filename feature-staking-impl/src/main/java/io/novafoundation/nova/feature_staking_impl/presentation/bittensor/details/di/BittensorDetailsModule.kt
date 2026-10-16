package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.di

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootClaimInteractor
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetRepository
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPriceChartInteractor
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicSubmissionRecovery
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorDetailsViewModel
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger

@Module(includes = [ViewModelModule::class])
class BittensorDetailsModule {

    @Provides
    fun providePriceChartInteractor(prices: CoinPriceRepository, logger: Logger) =
        BittensorPriceChartInteractor(prices, logger)

    @Provides
    @IntoMap
    @ViewModelKey(BittensorDetailsViewModel::class)
    fun provideViewModel(
        interactor: BittensorPortfolioInteractor,
        router: StakingRouter,
        resources: ResourceManager,
        amountFormatter: AmountFormatter,
        assetIconProvider: AssetIconProvider,
        iconGenerator: AddressIconGenerator,
        externalActions: ExternalActions.Presentation,
        priceCharts: BittensorPriceChartInteractor,
        logger: Logger,
        subnetRepository: BittensorSubnetRepository,
        recovery: ExtrinsicSubmissionRecovery,
        accountRepository: AccountRepository,
        claims: BittensorRootClaimInteractor
    ): ViewModel = BittensorDetailsViewModel(
        interactor = interactor,
        router = router,
        resources = resources,
        amountFormatter = amountFormatter,
        assetIconProvider = assetIconProvider,
        iconGenerator = iconGenerator,
        externalActions = externalActions,
        priceChartInteractor = priceCharts,
        logger = logger,
        subnetRepository = subnetRepository,
        recovery = recovery,
        accountRepository = accountRepository,
        claims = claims
    )

    @Provides
    fun provideViewModelCreator(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorDetailsViewModel =
        ViewModelProvider(fragment, factory)[BittensorDetailsViewModel::class.java]
}
