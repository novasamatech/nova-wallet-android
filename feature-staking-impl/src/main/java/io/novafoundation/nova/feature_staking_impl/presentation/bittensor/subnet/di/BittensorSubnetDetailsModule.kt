package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.di

import io.novafoundation.nova.analytics.AnalyticsService

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPresetValidatorInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorCommunicator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetDetailsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetParametersInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetSafetyInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorChartAxisFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetDetailsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetDetailsViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger

@Module(includes = [ViewModelModule::class])
class BittensorSubnetDetailsModule {

    @Provides
    @IntoMap
    @ViewModelKey(BittensorSubnetDetailsViewModel::class)
    fun provideViewModel(
        analytics: AnalyticsService,
        payload: BittensorSubnetDetailsPayload,
        bittensorSubnetInteractor: BittensorSubnetInteractor,
        bittensorSubnetStakingInteractor: BittensorSubnetStakingInteractor,
        router: StakingRouter,
        resources: ResourceManager,
        tokenFormatter: TokenFormatter,
        bittensorSubnetDetailsInteractor: BittensorSubnetDetailsInteractor,
        logger: Logger,
        fiatFormatter: FiatFormatter,
        chartAxisFormatterFactory: BittensorChartAxisFormatter.Factory,
        retryController: BittensorRetryController,
        subnetSafetyInteractor: BittensorSubnetSafetyInteractor,
        subnetParametersInteractor: BittensorSubnetParametersInteractor,
        validatorInteractor: BittensorValidatorInteractor,
        presetValidatorInteractor: BittensorPresetValidatorInteractor,
        validatorCommunicator: BittensorSelectValidatorCommunicator,
        addressIconGenerator: AddressIconGenerator,
        automaticInteractionGate: AutomaticInteractionGate
    ): ViewModel = BittensorSubnetDetailsViewModel(
        analytics, subnetId = BittensorNetuid.Subnet(payload.subnetId),
        bittensorSubnetInteractor = bittensorSubnetInteractor,
        bittensorSubnetStakingInteractor = bittensorSubnetStakingInteractor,
        router = router,
        resources = resources,
        tokenFormatter = tokenFormatter,
        bittensorSubnetDetailsInteractor = bittensorSubnetDetailsInteractor,
        logger = logger,
        fiatFormatter = fiatFormatter,
        chartAxisFormatterFactory = chartAxisFormatterFactory,
        retryController = retryController,
        subnetSafetyInteractor = subnetSafetyInteractor,
        subnetParametersInteractor = subnetParametersInteractor,
        validatorInteractor = validatorInteractor,
        presetValidatorInteractor = presetValidatorInteractor,
        validatorCommunicator = validatorCommunicator,
        addressIconGenerator = addressIconGenerator,
        automaticInteractionGate = automaticInteractionGate,
        replaceAmount = payload.replaceAmount,
        initialValidatorAddress = payload.validatorAddress,
        initialSelectionState = payload.selectionState,
        analyticsSource = payload.analyticsSource
    )

    @Provides
    fun provideViewModelCreator(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorSubnetDetailsViewModel =
        ViewModelProvider(fragment, factory)[BittensorSubnetDetailsViewModel::class.java]
}
