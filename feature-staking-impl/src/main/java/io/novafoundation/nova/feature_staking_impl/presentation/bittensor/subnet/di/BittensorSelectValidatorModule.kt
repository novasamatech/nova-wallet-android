package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.di

import io.novafoundation.nova.analytics.AnalyticsService

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
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorViewModel
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger

@Module(includes = [ViewModelModule::class])
class BittensorSelectValidatorModule {

    @Provides
    @IntoMap
    @ViewModelKey(BittensorSelectValidatorViewModel::class)
    fun selectValidator(
        analytics: AnalyticsService,
        request: BittensorSelectValidatorCommunicator.Request,
        communicator: BittensorSelectValidatorCommunicator,
        bittensorValidatorInteractor: BittensorValidatorInteractor,
        icons: AddressIconGenerator,
        router: StakingRouter,
        externalActions: ExternalActions.Presentation,
        resources: ResourceManager,
        logger: Logger,
        retryController: BittensorRetryController,
        tokenFormatter: TokenFormatter,
        subnetInteractor: BittensorSubnetInteractor
    ): ViewModel = BittensorSelectValidatorViewModel(
        analytics, request,
        communicator,
        bittensorValidatorInteractor,
        icons,
        router,
        externalActions,
        resources,
        logger,
        retryController = retryController,
        tokenFormatter = tokenFormatter,
        subnetInteractor = subnetInteractor
    )

    @Provides
    fun selectValidatorVm(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorSelectValidatorViewModel =
        ViewModelProvider(fragment, factory)[BittensorSelectValidatorViewModel::class.java]
}
