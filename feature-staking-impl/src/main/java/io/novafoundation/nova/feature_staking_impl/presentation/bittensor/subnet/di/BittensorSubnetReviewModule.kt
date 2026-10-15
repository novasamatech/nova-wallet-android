package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetReviewPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetReviewViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class BittensorSubnetReviewModule {

    @Provides
    @IntoMap
    @ViewModelKey(BittensorSubnetReviewViewModel::class)
    fun provideViewModel(
        payload: BittensorSubnetReviewPayload,
        bittensorSubnetInteractor: BittensorSubnetInteractor,
        router: StakingRouter,
        chainRegistry: ChainRegistry,
        walletUiUseCase: WalletUiUseCase,
        selectedAccountUseCase: SelectedAccountUseCase,
        bittensorSubnetStakingInteractor: BittensorSubnetStakingInteractor,
        resources: ResourceManager,
        icons: AssetIconProvider,
        externalActions: ExternalActions.Presentation,
        iconGenerator: AddressIconGenerator,
        retryController: BittensorRetryController,
        bittensorAmountFormatter: BittensorAmountFormatter,
        stakeValidationSystem: BittensorStakeValidationSystem
    ): ViewModel {
        return BittensorSubnetReviewViewModel(
            BittensorNetuid.Subnet(payload.subnetId),
            payload.amountInPlanks,
            payload.metaId,
            payload.validatorAddress,
            bittensorSubnetInteractor,
            router,
            chainRegistry,
            walletUiUseCase,
            selectedAccountUseCase,
            bittensorSubnetStakingInteractor,
            resources,
            icons,
            externalActions,
            iconGenerator,
            retryController = retryController,
            bittensorAmountFormatter = bittensorAmountFormatter,
            stakeValidationSystem = stakeValidationSystem
        )
    }

    @Provides
    fun provideViewModelCreator(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorSubnetReviewViewModel =
        ViewModelProvider(fragment, factory)[BittensorSubnetReviewViewModel::class.java]
}
