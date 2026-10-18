package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position.di

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
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorEstimatesMixin
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetDetailsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetMetadataInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position.BittensorSubnetPositionPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position.BittensorSubnetPositionViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger

@Module(includes = [ViewModelModule::class])
class BittensorSubnetPositionModule {
    @Provides
    @IntoMap
    @ViewModelKey(BittensorSubnetPositionViewModel::class)
    fun position(
        payload: BittensorSubnetPositionPayload,
        bittensorSubnetInteractor: BittensorSubnetInteractor,
        bittensorSubnetDetailsInteractor: BittensorSubnetDetailsInteractor,
        subnetMetadataInteractor: BittensorSubnetMetadataInteractor,
        router: StakingRouter,
        resources: ResourceManager,
        tokenFormatter: TokenFormatter,
        addressIconGenerator: AddressIconGenerator,
        externalActions: ExternalActions.Presentation,
        logger: Logger,
        fiatFormatter: FiatFormatter,
        estimatesMixin: BittensorEstimatesMixin,
        retryController: BittensorRetryController
    ): ViewModel =
        BittensorSubnetPositionViewModel(
            payload.subnetId(),
            bittensorSubnetInteractor,
            bittensorSubnetDetailsInteractor,
            subnetMetadataInteractor,
            router,
            resources,
            tokenFormatter,
            addressIconGenerator,
            externalActions,
            logger,
            fiatFormatter,
            estimatesMixin,
            retryController
        )

    @Provides
    fun positionVm(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorSubnetPositionViewModel =
        ViewModelProvider(fragment, factory)[BittensorSubnetPositionViewModel::class.java]
}
