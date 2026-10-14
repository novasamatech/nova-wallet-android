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
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetAmountViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetAmountPayload
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter

@Module(includes = [ViewModelModule::class])
class BittensorSubnetAmountModule {

    @Provides
    @IntoMap
    @ViewModelKey(BittensorSubnetAmountViewModel::class)
    fun provideViewModel(
        payload: BittensorSubnetAmountPayload,
        bittensorSubnetInteractor: BittensorSubnetInteractor,
        router: StakingRouter,
        resources: ResourceManager,
        icons: AssetIconProvider,
        tokenFormatter: TokenFormatter,
        logger: Logger,
        bittensorSubnetStakingInteractor: BittensorSubnetStakingInteractor,
        fiatFormatter: FiatFormatter
    ): ViewModel = BittensorSubnetAmountViewModel(
        initialSubnetId = BittensorSubnetId(payload.subnetId),
        validatorAddress = payload.validatorAddress,
        bittensorSubnetInteractor = bittensorSubnetInteractor,
        router = router,
        resources = resources,
        icons = icons,
        tokenFormatter = tokenFormatter,
        logger = logger,
        bittensorSubnetStakingInteractor = bittensorSubnetStakingInteractor,
        fiatFormatter = fiatFormatter
    )

    @Provides
    fun provideViewModelCreator(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorSubnetAmountViewModel =
        ViewModelProvider(fragment, factory)[BittensorSubnetAmountViewModel::class.java]
}
