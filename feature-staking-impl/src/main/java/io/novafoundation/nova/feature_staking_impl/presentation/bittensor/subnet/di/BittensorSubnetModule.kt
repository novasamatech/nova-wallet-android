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
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorChooseSubnetViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter

@Module(includes = [ViewModelModule::class])
class BittensorSubnetModule {

    @Provides
    @IntoMap
    @ViewModelKey(BittensorChooseSubnetViewModel::class)
    fun chooser(
        bittensorSubnetInteractor: BittensorSubnetInteractor,
        router: StakingRouter,
        icons: AssetIconProvider,
        tokenFormatter: TokenFormatter,
        retryController: BittensorRetryController
    ): ViewModel = BittensorChooseSubnetViewModel(
        bittensorSubnetInteractor,
        router,
        icons,
        tokenFormatter,
        retryController = retryController
    )

    @Provides
    fun chooserVm(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorChooseSubnetViewModel =
        ViewModelProvider(fragment, factory)[BittensorChooseSubnetViewModel::class.java]
}
