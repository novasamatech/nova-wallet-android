package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount.di

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
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount.BittensorStakeAmountViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorCommunicator
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin

@Module(includes = [ViewModelModule::class])
class BittensorStakeAmountModule {

    @Provides
    @IntoMap
    @ViewModelKey(BittensorStakeAmountViewModel::class)
    fun provideViewModel(
        resourceManager: ResourceManager,
        router: StakingRouter,
        bittensorStakingInteractor: BittensorStakingInteractor,
        iconGenerator: AddressIconGenerator,
        validatorCommunicator: BittensorSelectValidatorCommunicator,
        fiatFormatter: FiatFormatter,
        tokenFormatter: TokenFormatter,
        retryController: BittensorRetryController,
        bittensorAmountFormatter: BittensorAmountFormatter,
        amountChooserFactory: AmountChooserMixin.Factory,
        stakeValidationSystem: BittensorStakePreflightSystem,
        validationExecutor: ValidationExecutor
    ): ViewModel {
        return BittensorStakeAmountViewModel(
            resourceManager = resourceManager,
            router = router,
            bittensorStakingInteractor = bittensorStakingInteractor,
            iconGenerator = iconGenerator,
            validatorCommunicator = validatorCommunicator,
            fiatFormatter = fiatFormatter,
            tokenFormatter = tokenFormatter,
            retryController = retryController,
            bittensorAmountFormatter = bittensorAmountFormatter,
            amountChooserFactory = amountChooserFactory,
            stakeValidationSystem = stakeValidationSystem,
            validationExecutor = validationExecutor
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
