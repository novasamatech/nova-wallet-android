package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioHistoryApi
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioHistoryRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootValidatorSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetMarketRepository
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioChartCalculator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioChartInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPositionUpdates
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioRowInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootClaimInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorChartAxisFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorDetailsViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorPortfolioRowMapper
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorRootDetailsViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorRootHoldFormatter
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.infrastructure.di.Attested
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class BittensorDetailsModule {

    @Provides
    fun providePortfolioRowInteractor(
        rootValidators: BittensorRootValidatorSource,
        markets: BittensorSubnetMarketRepository,
        prices: CoinPriceRepository,
        currencies: CurrencyRepository
    ) = BittensorPortfolioRowInteractor(rootValidators, markets, prices, currencies)

    @Provides
    fun providePortfolioRowMapper(resources: ResourceManager, amounts: BittensorAmountFormatter, icons: AssetIconProvider) =
        BittensorPortfolioRowMapper(resources, amounts, icons)

    @Provides
    fun providePortfolioHistoryApi(@Attested apiCreator: NetworkApiCreator): BittensorPortfolioHistoryApi =
        apiCreator.create(BittensorPortfolioHistoryApi::class.java)

    @Provides
    fun providePortfolioHistoryRepository(
        prices: CoinPriceRepository,
        api: BittensorPortfolioHistoryApi,
        urls: InfrastructureUrls
    ) = BittensorPortfolioHistoryRepository(api, urls, prices)

    @Provides
    fun providePortfolioChartCalculator() = BittensorPortfolioChartCalculator()

    @Provides
    fun providePortfolioChartInteractor(
        historyRepository: BittensorPortfolioHistoryRepository,
        calculator: BittensorPortfolioChartCalculator,
        accounts: AccountRepository,
        chainRegistry: ChainRegistry,
        positionUpdates: BittensorPositionUpdates
    ) = BittensorPortfolioChartInteractor(historyRepository, calculator, accounts, chainRegistry, positionUpdates)

    @Provides
    @IntoMap
    @ViewModelKey(BittensorDetailsViewModel::class)
    fun provideViewModel(
        bittensorPortfolioInteractor: BittensorPortfolioInteractor,
        router: StakingRouter,
        resources: ResourceManager,
        amountFormatter: BittensorAmountFormatter,
        rowMapper: BittensorPortfolioRowMapper,
        externalActions: ExternalActions.Presentation,
        bittensorPortfolioChartInteractor: BittensorPortfolioChartInteractor,
        retryController: BittensorRetryController,
        chartAxisFormatterFactory: BittensorChartAxisFormatter.Factory,
        rowInteractor: BittensorPortfolioRowInteractor,
        rootClaimInteractor: BittensorRootClaimInteractor
    ): ViewModel = BittensorDetailsViewModel(
        bittensorPortfolioInteractor = bittensorPortfolioInteractor,
        router = router,
        resources = resources,
        amountFormatter = amountFormatter,
        rowMapper = rowMapper,
        externalActions = externalActions,
        bittensorPortfolioChartInteractor = bittensorPortfolioChartInteractor,
        retryController = retryController,
        chartAxisFormatterFactory = chartAxisFormatterFactory,
        rowInteractor = rowInteractor,
        rootClaimInteractor = rootClaimInteractor
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
        retryController: BittensorRetryController,
        rootClaimInteractor: BittensorRootClaimInteractor,
        accountRepository: AccountRepository,
        withdrawalInteractor: io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPositionWithdrawalInteractor,
        slippageSettings: io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorSlippageSettings,
        rootValidatorSource: BittensorRootValidatorSource,
        rootHoldFormatter: BittensorRootHoldFormatter,
        debugSettings: io.novafoundation.nova.common.data.storage.DebugSettings
    ): ViewModel = BittensorRootDetailsViewModel(
        interactor,
        router,
        resources,
        icons,
        addressIconGenerator,
        externalActions,
        amountFormatter,
        retryController,
        rootClaimInteractor = rootClaimInteractor,
        accountRepository = accountRepository,
        withdrawalInteractor = withdrawalInteractor,
        slippageSettings = slippageSettings,
        rootValidatorSource = rootValidatorSource,
        rootHoldFormatter = rootHoldFormatter,
        debugSettings = debugSettings
    )

    @Provides
    fun provideRootViewModelCreator(fragment: Fragment, factory: ViewModelProvider.Factory):
        BittensorRootDetailsViewModel =
        ViewModelProvider(fragment, factory)[BittensorRootDetailsViewModel::class.java]

    @Provides
    fun provideViewModelCreator(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorDetailsViewModel =
        ViewModelProvider(fragment, factory)[BittensorDetailsViewModel::class.java]
}
