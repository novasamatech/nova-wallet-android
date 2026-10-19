package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.di

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorChooseSubnetPayload

import androidx.fragment.app.Fragment
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.input.chooser.ListChooserMixin
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetBrowseRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetCatalogueApi
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootValidatorSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetMarketRepository
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioRowInteractor
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetTradingAvailabilityRepository
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRecommendedYieldInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorChooseSubnetViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorWhereToStakeViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter

@Module(includes = [ViewModelModule::class, BittensorQuickPickModule::class])
class BittensorSubnetModule {

    @Provides
    fun browseRepository(
        api: BittensorSubnetCatalogueApi,
        urls: InfrastructureUrls,
        repository: BittensorSubnetRepository,
        availability: BittensorSubnetTradingAvailabilityRepository
    ) = BittensorSubnetBrowseRepository(api, urls, repository, availability)

    @Provides
    fun browseRowInteractor(
        rootValidators: BittensorRootValidatorSource,
        markets: BittensorSubnetMarketRepository,
        prices: CoinPriceRepository,
        currencies: CurrencyRepository
    ) = BittensorPortfolioRowInteractor(rootValidators, markets, prices, currencies)

    @Provides
    @IntoMap
    @ViewModelKey(BittensorChooseSubnetViewModel::class)
    fun chooser(
        bittensorSubnetInteractor: BittensorSubnetInteractor,
        router: StakingRouter,
        icons: AssetIconProvider,
        tokenFormatter: TokenFormatter,
        retryController: BittensorRetryController,
        browseRepository: BittensorSubnetBrowseRepository,
        recommendedYields: BittensorRecommendedYieldInteractor,
        rowInteractor: BittensorPortfolioRowInteractor,
        resources: ResourceManager,
        listChooserMixinFactory: ListChooserMixin.Factory,
        payload: BittensorChooseSubnetPayload
    ): ViewModel = BittensorChooseSubnetViewModel(
        bittensorSubnetInteractor,
        router,
        icons,
        tokenFormatter,
        retryController = retryController,
        browseRepository = browseRepository,
        recommendedYields = recommendedYields,
        rowInteractor = rowInteractor,
        resources = resources,
        listChooserMixinFactory = listChooserMixinFactory,
        payload = payload
    )

    @Provides
    fun chooserVm(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorChooseSubnetViewModel =
        ViewModelProvider(fragment, factory)[BittensorChooseSubnetViewModel::class.java]

    @Provides
    @IntoMap
    @ViewModelKey(BittensorWhereToStakeViewModel::class)
    fun whereToStake(
        bittensorSubnetInteractor: BittensorSubnetInteractor,
        router: StakingRouter,
        retryController: BittensorRetryController
    ): ViewModel = BittensorWhereToStakeViewModel(bittensorSubnetInteractor, router, retryController)

    @Provides
    fun whereToStakeVm(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorWhereToStakeViewModel =
        ViewModelProvider(fragment, factory)[BittensorWhereToStakeViewModel::class.java]
}
