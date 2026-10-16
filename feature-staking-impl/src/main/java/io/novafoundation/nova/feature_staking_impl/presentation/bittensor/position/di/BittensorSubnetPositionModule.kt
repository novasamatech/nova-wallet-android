package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position.di

import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position.BittensorSubnetPositionViewModel
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetDetailsInteractor
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorEstimatesRepository
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetMetadataInteractor
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicSubmissionRecovery
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position.BittensorSubnetPositionPayload

@Module(includes = [ViewModelModule::class])
class BittensorSubnetPositionModule {
    @Provides
    @IntoMap
    @ViewModelKey(BittensorSubnetPositionViewModel::class)
    fun position(
        payload: BittensorSubnetPositionPayload,
        interactor: BittensorSubnetInteractor,
        details: BittensorSubnetDetailsInteractor,
        metadata: BittensorSubnetMetadataInteractor,
        router: StakingRouter,
        resources: ResourceManager,
        tokens: TokenFormatter,
        addresses: AddressIconGenerator,
        recovery: ExtrinsicSubmissionRecovery,
        externalActions: ExternalActions.Presentation,
        logger: Logger,
        fiatFormatter: FiatFormatter,
        estimates: BittensorEstimatesRepository
    ): ViewModel =
        BittensorSubnetPositionViewModel(
            payload.subnetId(), interactor,
            details, metadata, router, resources, tokens, addresses, recovery, externalActions, logger, fiatFormatter, estimates
        )

    @Provides
    fun positionVm(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorSubnetPositionViewModel =
        ViewModelProvider(fragment, factory)[BittensorSubnetPositionViewModel::class.java]
}
