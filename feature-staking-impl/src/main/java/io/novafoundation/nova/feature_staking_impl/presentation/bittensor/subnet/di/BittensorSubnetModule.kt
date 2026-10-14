package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorChooseSubnetViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorViewModel

@Module(includes = [ViewModelModule::class])
class BittensorSubnetModule {

    @Provides
    @IntoMap
    @ViewModelKey(BittensorChooseSubnetViewModel::class)
    fun chooser(
        bittensorSubnetInteractor: BittensorSubnetInteractor,
        router: StakingRouter,
        icons: AssetIconProvider,
        logger: Logger,
        tokenFormatter: TokenFormatter
    ): ViewModel = BittensorChooseSubnetViewModel(bittensorSubnetInteractor, router, icons, logger, tokenFormatter)

    @Provides
    @IntoMap
    @ViewModelKey(BittensorSelectValidatorViewModel::class)
    fun selectValidator(
        communicator: BittensorSelectValidatorCommunicator,
        bittensorSubnetStakingInteractor: BittensorSubnetStakingInteractor,
        icons: AddressIconGenerator,
        router: StakingRouter,
        externalActions: ExternalActions.Presentation,
        chainRegistry: ChainRegistry,
        resources: ResourceManager,
        logger: Logger
    ): ViewModel = BittensorSelectValidatorViewModel(
        communicator,
        bittensorSubnetStakingInteractor,
        icons,
        router,
        externalActions,
        chainRegistry,
        resources,
        logger
    )

    @Provides
    fun selectValidatorVm(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorSelectValidatorViewModel =
        ViewModelProvider(fragment, factory)[BittensorSelectValidatorViewModel::class.java]

    @Provides
    fun chooserVm(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorChooseSubnetViewModel =
        ViewModelProvider(fragment, factory)[BittensorChooseSubnetViewModel::class.java]
}
