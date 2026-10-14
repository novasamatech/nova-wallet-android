package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount.BittensorStakeAmountViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorCommunicator
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter

@Module(includes = [ViewModelModule::class])
class BittensorStakeAmountModule {

    @Provides
    @IntoMap
    @ViewModelKey(BittensorStakeAmountViewModel::class)
    fun provideViewModel(
        resourceManager: ResourceManager,
        router: StakingRouter,
        bittensorStakingInteractor: BittensorStakingInteractor,
        assetIconProvider: AssetIconProvider,
        iconGenerator: AddressIconGenerator,
        validatorCommunicator: BittensorSelectValidatorCommunicator,
        fiatFormatter: FiatFormatter,
        tokenFormatter: TokenFormatter
    ): ViewModel {
        return BittensorStakeAmountViewModel(
            resourceManager = resourceManager,
            router = router,
            bittensorStakingInteractor = bittensorStakingInteractor,
            assetIconProvider = assetIconProvider,
            iconGenerator = iconGenerator,
            validatorCommunicator = validatorCommunicator,
            fiatFormatter = fiatFormatter,
            tokenFormatter = tokenFormatter
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): BittensorStakeAmountViewModel {
        return ViewModelProvider(fragment, viewModelFactory)[BittensorStakeAmountViewModel::class.java]
    }
}
