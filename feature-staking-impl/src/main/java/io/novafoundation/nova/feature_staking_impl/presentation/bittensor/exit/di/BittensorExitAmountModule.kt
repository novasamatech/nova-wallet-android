package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.di

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorSaleCostBasisFormatter

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
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorEstimatesMixin
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorSlippageSettings
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitAmountViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitPositionPayload
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2

@Module(includes = [ViewModelModule::class])
class BittensorExitAmountModule {

    @Provides
    @IntoMap
    @ViewModelKey(BittensorExitAmountViewModel::class)
    fun amount(
        payload: BittensorExitPositionPayload,
        bittensorExitInteractor: BittensorExitInteractor,
        router: StakingRouter,
        resources: ResourceManager,
        assetIconProvider: AssetIconProvider,
        addressIconGenerator: AddressIconGenerator,
        tokenFormatter: TokenFormatter,
        fiatFormatter: FiatFormatter,
        externalActions: ExternalActions.Presentation,
        estimatesMixin: BittensorEstimatesMixin,
        retryController: BittensorRetryController,
        validationExecutor: ValidationExecutor,
        exitValidationSystem: BittensorExitValidationSystem,
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
        exitFormatter: BittensorExitFormatter,
        slippageSettings: BittensorSlippageSettings,
        slippageSettingsCommunicator: SlippageSettingsCommunicator,
        costBasisFormatter: BittensorSaleCostBasisFormatter
    ): ViewModel {
        return BittensorExitAmountViewModel(
            payload,
            bittensorExitInteractor,
            router,
            resources,
            assetIconProvider,
            addressIconGenerator,
            tokenFormatter,
            fiatFormatter,
            externalActions,
            estimatesMixin,
            retryController,
            validationExecutor,
            exitValidationSystem,
            feeLoaderMixinFactory,
            exitFormatter,
            slippageSettings = slippageSettings,
            slippageSettingsCommunicator = slippageSettingsCommunicator,
            costBasisFormatter = costBasisFormatter
        )
    }

    @Provides
    fun amountVm(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorExitAmountViewModel =
        ViewModelProvider(fragment, factory)[BittensorExitAmountViewModel::class.java]
}
