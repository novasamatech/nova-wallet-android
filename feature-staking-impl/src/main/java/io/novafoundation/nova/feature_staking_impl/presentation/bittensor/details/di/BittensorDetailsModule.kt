package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.di

import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
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
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioHistoryRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPositionUpdates
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioHistoryInteractor
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorDetailsViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter

@Module(includes = [ViewModelModule::class])
class BittensorDetailsModule {

    @Provides
    fun provideHistoryInteractor(
        repository: BittensorPortfolioHistoryRepository,
        prices: CoinPriceRepository,
        positionUpdates: BittensorPositionUpdates,
        logger: Logger
    ) = BittensorPortfolioHistoryInteractor(repository, prices, positionUpdates, logger)

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
        historyInteractor: BittensorPortfolioHistoryInteractor,
        logger: Logger,
        subnetRepository: BittensorSubnetRepository
    ): ViewModel = BittensorDetailsViewModel(
        interactor = interactor,
        router = router,
        resources = resources,
        amountFormatter = amountFormatter,
        assetIconProvider = assetIconProvider,
        iconGenerator = iconGenerator,
        externalActions = externalActions,
        historyInteractor = historyInteractor,
        logger = logger,
        subnetRepository = subnetRepository
    )

    @Provides
    fun provideViewModelCreator(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorDetailsViewModel =
        ViewModelProvider(fragment, factory)[BittensorDetailsViewModel::class.java]
}
