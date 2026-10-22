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
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorQuickPickInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPresetValidatorInteractor
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorAlphaYieldRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorSaleCostBasisFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorEstimatesMixin
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorSlippageSettings
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetAmountPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetAmountViewModel
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset.GetAssetOptionsMixin
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetFactsInteractor
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorBackend
import io.novafoundation.nova.runtime.repository.ChainStateRepository

@Module(includes = [ViewModelModule::class, BittensorQuickPickModule::class])
class BittensorSubnetAmountModule {

    @Provides
    fun subnetFacts(backend: BittensorValidatorBackend, chainStateRepository: ChainStateRepository) =
        BittensorSubnetFactsInteractor(backend, chainStateRepository)

    @Provides
    fun presetValidators(
        repository: BittensorSubnetValidatorRepository,
        yields: BittensorAlphaYieldRepository,
        chains: ChainRegistry,
        logger: Logger
    ) = BittensorPresetValidatorInteractor(repository, yields, chains, logger)

    @Provides
    @IntoMap
    @ViewModelKey(BittensorSubnetAmountViewModel::class)
    fun provideViewModel(
        analytics: AnalyticsService,
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
        amountChooserFactory: AmountChooserMixin.Factory,
        validationSystem: BittensorStakePreflightSystem,
        validationExecutor: ValidationExecutor,
        feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
        estimatesMixin: BittensorEstimatesMixin,
        slippageSettings: BittensorSlippageSettings,
        slippageSettingsCommunicator: SlippageSettingsCommunicator,
        validatorCommunicator: BittensorSelectValidatorCommunicator,
        getAssetOptionsMixinFactory: GetAssetOptionsMixin.Factory,
        costBasisFormatter: BittensorSaleCostBasisFormatter,
        quickPickInteractor: BittensorQuickPickInteractor,
        presetValidatorInteractor: BittensorPresetValidatorInteractor,
        subnetFactsInteractor: BittensorSubnetFactsInteractor
    ): ViewModel = BittensorSubnetAmountViewModel(
        analytics, initialSubnetId = payload.subnetId?.let { BittensorNetuid.Subnet(it) },
        quickPickOrder = payload.quickPickOrder,
        quickPickInteractor = quickPickInteractor,
        presetValidatorInteractor = presetValidatorInteractor,
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
        amountChooserFactory = amountChooserFactory,
        validationSystem = validationSystem,
        validationExecutor = validationExecutor,
        feeLoaderMixinFactory = feeLoaderMixinFactory,
        existingPosition = payload.existingPosition,
        costBasisFormatter = costBasisFormatter,
        estimatesMixin = estimatesMixin,
        slippageSettings = slippageSettings,
        slippageSettingsCommunicator = slippageSettingsCommunicator,
        validatorCommunicator = validatorCommunicator,
        getAssetOptionsMixinFactory = getAssetOptionsMixinFactory,
        subnetFactsInteractor = subnetFactsInteractor,
        initialSelectionState = payload.selectionState
    )

    @Provides
    fun provideViewModelCreator(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorSubnetAmountViewModel =
        ViewModelProvider(fragment, factory)[BittensorSubnetAmountViewModel::class.java]
}
