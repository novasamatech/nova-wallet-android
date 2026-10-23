package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.di

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
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorDelegateSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetCatalogueApi
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorSubnetCatalogue
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorCoverageInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.BittensorValidatorInfoPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.BittensorValidatorInfoViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger

@Module(includes = [ViewModelModule::class])
class BittensorValidatorInfoModule {

    @Provides
    fun provideSubnetCatalogue(api: BittensorSubnetCatalogueApi, urls: InfrastructureUrls): BittensorValidatorSubnetCatalogue =
        BittensorValidatorSubnetCatalogue(api, urls)

    @Provides
    fun provideCoverageInteractor(
        delegates: BittensorDelegateSource,
        catalogue: BittensorValidatorSubnetCatalogue
    ): BittensorValidatorCoverageInteractor = BittensorValidatorCoverageInteractor(delegates, catalogue)

    @Provides
    @IntoMap
    @ViewModelKey(BittensorValidatorInfoViewModel::class)
    fun provideViewModel(
        payload: BittensorValidatorInfoPayload,
        router: StakingRouter,
        validatorInteractor: BittensorValidatorInteractor,
        coverageInteractor: BittensorValidatorCoverageInteractor,
        subnetInteractor: BittensorSubnetInteractor,
        subnetCatalogue: BittensorValidatorSubnetCatalogue,
        addressIconGenerator: AddressIconGenerator,
        amountFormatter: BittensorAmountFormatter,
        tokenFormatter: TokenFormatter,
        externalActions: ExternalActions.Presentation,
        resources: ResourceManager,
        logger: Logger
    ): ViewModel = BittensorValidatorInfoViewModel(
        payload = payload,
        router = router,
        validatorInteractor = validatorInteractor,
        coverageInteractor = coverageInteractor,
        subnetInteractor = subnetInteractor,
        subnetCatalogue = subnetCatalogue,
        addressIconGenerator = addressIconGenerator,
        amountFormatter = amountFormatter,
        tokenFormatter = tokenFormatter,
        externalActions = externalActions,
        resources = resources,
        logger = logger
    )

    @Provides
    fun provideViewModelCreator(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorValidatorInfoViewModel =
        ViewModelProvider(fragment, factory)[BittensorValidatorInfoViewModel::class.java]
}
