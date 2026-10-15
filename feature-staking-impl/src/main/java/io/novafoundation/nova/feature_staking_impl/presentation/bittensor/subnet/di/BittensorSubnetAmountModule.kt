package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetAmountPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetAmountViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger

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
        tokenFormatter: TokenFormatter,
        logger: Logger,
        bittensorSubnetStakingInteractor: BittensorSubnetStakingInteractor,
        fiatFormatter: FiatFormatter,
        retryController: BittensorRetryController,
        bittensorAmountFormatter: BittensorAmountFormatter,
        amountChooserFactory: AmountChooserMixin.Factory
    ): ViewModel = BittensorSubnetAmountViewModel(
        initialSubnetId = BittensorNetuid.Subnet(payload.subnetId),
        validatorAddress = payload.validatorAddress,
        bittensorSubnetInteractor = bittensorSubnetInteractor,
        router = router,
        resources = resources,
        tokenFormatter = tokenFormatter,
        logger = logger,
        bittensorSubnetStakingInteractor = bittensorSubnetStakingInteractor,
        fiatFormatter = fiatFormatter,
        retryController = retryController,
        bittensorAmountFormatter = bittensorAmountFormatter,
        amountChooserFactory = amountChooserFactory
    )

    @Provides
    fun provideViewModelCreator(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorSubnetAmountViewModel =
        ViewModelProvider(fragment, factory)[BittensorSubnetAmountViewModel::class.java]
}
