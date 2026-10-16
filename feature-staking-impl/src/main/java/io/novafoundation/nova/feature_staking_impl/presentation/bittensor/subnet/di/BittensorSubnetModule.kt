package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.di

import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorEstimatesRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootValidatorSelection
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
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicSubmissionRecovery
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetDetailsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorChooseSubnetViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetAmountViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetDetailsViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetReviewViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigInteger

@Module(includes = [ViewModelModule::class])
class BittensorSubnetModule {

    @Provides
    @IntoMap
    @ViewModelKey(BittensorChooseSubnetViewModel::class)
    fun chooser(interactor: BittensorSubnetInteractor, router: StakingRouter, icons: AssetIconProvider): ViewModel =
        BittensorChooseSubnetViewModel(interactor, router, icons)

    @Provides
    @IntoMap
    @ViewModelKey(BittensorSubnetAmountViewModel::class)
    fun amount(
        fragment: Fragment,
        interactor: BittensorSubnetInteractor,
        router: StakingRouter,
        resources: ResourceManager,
        icons: AssetIconProvider,
        tokenFormatter: TokenFormatter,
        logger: Logger,
        staking: BittensorSubnetStakingInteractor,
        estimates: BittensorEstimatesRepository
    ): ViewModel = BittensorSubnetAmountViewModel(
        BittensorSubnetId(fragment.requireArguments().getInt("subnetId")),
        requireNotNull(fragment.requireArguments().getString("validatorAddress")),
        interactor,
        router,
        resources,
        icons,
        tokenFormatter,
        logger,
        staking,
        fragment.requireArguments().getBoolean("existingPosition"),
        estimates
    )

    @Provides
    @IntoMap
    @ViewModelKey(BittensorSubnetReviewViewModel::class)
    fun review(
        fragment: Fragment,
        interactor: BittensorSubnetInteractor,
        router: StakingRouter,
        chainRegistry: ChainRegistry,
        walletUiUseCase: WalletUiUseCase,
        selectedAccountUseCase: SelectedAccountUseCase,
        staking: BittensorSubnetStakingInteractor,
        resources: ResourceManager,
        icons: AssetIconProvider,
        tokenFormatter: TokenFormatter,
        amountFormatter: AmountFormatter,
        externalActions: ExternalActions.Presentation,
        logger: Logger,
        iconGenerator: AddressIconGenerator,
        chainStateRepository: ChainStateRepository,
        recovery: ExtrinsicSubmissionRecovery,
        estimates: BittensorEstimatesRepository
    ): ViewModel = with(fragment.requireArguments()) {
        require(containsKey("metaId"))
        BittensorSubnetReviewViewModel(
            BittensorSubnetId(getInt("subnetId")),
            BigInteger(requireNotNull(getString("amount"))),
            getLong("metaId"),
            requireNotNull(getString("validatorAddress")),
            interactor,
            router,
            chainRegistry,
            walletUiUseCase,
            selectedAccountUseCase,
            staking,
            resources,
            icons,
            tokenFormatter,
            amountFormatter,
            externalActions,
            logger,
            iconGenerator,
            chainStateRepository,
            recovery,
            estimates
        )
    }

    @Provides
    @IntoMap
    @ViewModelKey(BittensorSubnetDetailsViewModel::class)
    fun details(
        fragment: Fragment,
        interactor: BittensorSubnetInteractor,
        staking: BittensorSubnetStakingInteractor,
        router: StakingRouter,
        resources: ResourceManager,
        tokenFormatter: TokenFormatter,
        iconGenerator: AddressIconGenerator,
        details: BittensorSubnetDetailsInteractor,
        logger: Logger,
        validatorCommunicator: BittensorSelectValidatorCommunicator,
        rpc: RpcCalls,
        chainState: ChainStateRepository
    ): ViewModel = BittensorSubnetDetailsViewModel(
        BittensorSubnetId(fragment.requireArguments().getInt("subnetId")),
        interactor, staking, router, resources, tokenFormatter, iconGenerator, details, logger, validatorCommunicator, rpc, chainState
    )

    @Provides
    @IntoMap
    @ViewModelKey(BittensorSelectValidatorViewModel::class)
    fun selectValidator(
        communicator: BittensorSelectValidatorCommunicator,
        staking: BittensorSubnetStakingInteractor,
        icons: AddressIconGenerator,
        router: StakingRouter,
        externalActions: ExternalActions.Presentation,
        chainRegistry: ChainRegistry,
        resources: ResourceManager,
        logger: Logger,
        rootSelection: BittensorRootValidatorSelection
    ): ViewModel = BittensorSelectValidatorViewModel(communicator, staking, icons, router, externalActions, chainRegistry, resources, logger, rootSelection)

    @Provides
    fun selectValidatorVm(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorSelectValidatorViewModel =
        ViewModelProvider(fragment, factory)[BittensorSelectValidatorViewModel::class.java]

    @Provides
    fun detailsVm(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorSubnetDetailsViewModel =
        ViewModelProvider(fragment, factory)[BittensorSubnetDetailsViewModel::class.java]

    @Provides
    fun chooserVm(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorChooseSubnetViewModel =
        ViewModelProvider(fragment, factory)[BittensorChooseSubnetViewModel::class.java]

    @Provides
    fun amountVm(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorSubnetAmountViewModel =
        ViewModelProvider(fragment, factory)[BittensorSubnetAmountViewModel::class.java]

    @Provides
    fun reviewVm(fragment: Fragment, factory: ViewModelProvider.Factory): BittensorSubnetReviewViewModel =
        ViewModelProvider(fragment, factory)[BittensorSubnetReviewViewModel::class.java]
}
