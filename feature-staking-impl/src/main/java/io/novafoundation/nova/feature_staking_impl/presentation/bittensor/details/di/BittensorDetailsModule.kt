package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.di

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
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPriceChartInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorChartAxisFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorDetailsViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorRootDetailsViewModel
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
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
        bittensorPortfolioInteractor: BittensorPortfolioInteractor,
        router: StakingRouter,
        resources: ResourceManager,
        amountFormatter: BittensorAmountFormatter,
        assetIconProvider: AssetIconProvider,
        externalActions: ExternalActions.Presentation,
        bittensorPriceChartInteractor: BittensorPriceChartInteractor,
        logger: Logger,
        chartAxisFormatterFactory: BittensorChartAxisFormatter.Factory,
        fiatFormatter: FiatFormatter
    ): ViewModel = BittensorDetailsViewModel(
        bittensorPortfolioInteractor = bittensorPortfolioInteractor,
        router = router,
        resources = resources,
        amountFormatter = amountFormatter,
        assetIconProvider = assetIconProvider,
        externalActions = externalActions,
        bittensorPriceChartInteractor = bittensorPriceChartInteractor,
        logger = logger,
        chartAxisFormatterFactory = chartAxisFormatterFactory,
        fiatFormatter = fiatFormatter
    )

    @Provides
    @IntoMap
    @ViewModelKey(BittensorRootDetailsViewModel::class)
    fun provideRootViewModel(
        interactor: BittensorPortfolioInteractor,
        router: StakingRouter,
        resources: ResourceManager,
        icons: AssetIconProvider,
        addressIconGenerator: AddressIconGenerator,
        externalActions: ExternalActions.Presentation,
        amountFormatter: BittensorAmountFormatter,
        retryController: BittensorRetryController
    ): ViewModel = BittensorRootDetailsViewModel(
        interactor,
        router,
        resources,
        icons,
        addressIconGenerator,
        externalActions,
        amountFormatter,
        retryController
    )

    @Provides
    fun provideRootViewModelCreator(fragment: Fragment, factory: ViewModelProvider.Factory):
        BittensorRootDetailsViewModel =
        ViewModelProvider(fragment, factory)[BittensorRootDetailsViewModel::class.java]

    @Provides
    fun provideViewModelCreator(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorDetailsViewModel =
        ViewModelProvider(fragment, factory)[BittensorDetailsViewModel::class.java]
}
