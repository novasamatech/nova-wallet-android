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
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetDetailsViewModel
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetDetailsInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorChooseSubnetViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetAmountViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetReviewViewModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorChartAxisFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import java.math.BigInteger
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorViewModel

@Module(includes = [ViewModelModule::class])
class BittensorSubnetModule {

    @Provides
    @IntoMap
    @ViewModelKey(BittensorChooseSubnetViewModel::class)
    fun chooser(
        bittensorSubnetInteractor: BittensorSubnetInteractor,
        router: StakingRouter,
        icons: AssetIconProvider,
        logger: Logger,
        tokenFormatter: TokenFormatter
    ): ViewModel = BittensorChooseSubnetViewModel(bittensorSubnetInteractor, router, icons, logger, tokenFormatter)

    @Provides
    @IntoMap
    @ViewModelKey(BittensorSubnetAmountViewModel::class)
    fun amount(
        fragment: Fragment,
        bittensorSubnetInteractor: BittensorSubnetInteractor,
        router: StakingRouter,
        resources: ResourceManager,
        icons: AssetIconProvider,
        tokenFormatter: TokenFormatter,
        logger: Logger,
        bittensorSubnetStakingInteractor: BittensorSubnetStakingInteractor
    ): ViewModel = BittensorSubnetAmountViewModel(
        BittensorSubnetId(fragment.requireArguments().getInt("subnetId")),
        requireNotNull(fragment.requireArguments().getString("validatorAddress")),
        bittensorSubnetInteractor,
        router,
        resources,
        icons,
        tokenFormatter,
        logger,
        bittensorSubnetStakingInteractor
    )

    @Provides
    @IntoMap
    @ViewModelKey(BittensorSubnetReviewViewModel::class)
    fun review(
        fragment: Fragment,
        bittensorSubnetInteractor: BittensorSubnetInteractor,
        router: StakingRouter,
        chainRegistry: ChainRegistry,
        walletUiUseCase: WalletUiUseCase,
        selectedAccountUseCase: SelectedAccountUseCase,
        bittensorSubnetStakingInteractor: BittensorSubnetStakingInteractor,
        resources: ResourceManager,
        icons: AssetIconProvider,
        tokenFormatter: TokenFormatter,
        amountFormatter: AmountFormatter,
        externalActions: ExternalActions.Presentation,
        logger: Logger,
        iconGenerator: AddressIconGenerator
    ): ViewModel = with(fragment.requireArguments()) {
        require(containsKey("metaId"))
        BittensorSubnetReviewViewModel(
            BittensorSubnetId(getInt("subnetId")),
            BigInteger(requireNotNull(getString("amount"))),
            getLong("metaId"),
            requireNotNull(getString("validatorAddress")),
            bittensorSubnetInteractor,
            router,
            chainRegistry,
            walletUiUseCase,
            selectedAccountUseCase,
            bittensorSubnetStakingInteractor,
            resources,
            icons,
            tokenFormatter,
            amountFormatter,
            externalActions,
            logger,
            iconGenerator
        )
    }

    @Provides
    @IntoMap
    @ViewModelKey(BittensorSubnetDetailsViewModel::class)
    fun details(
        fragment: Fragment,
        bittensorSubnetInteractor: BittensorSubnetInteractor,
        bittensorSubnetStakingInteractor: BittensorSubnetStakingInteractor,
        router: StakingRouter,
        resources: ResourceManager,
        tokenFormatter: TokenFormatter,
        iconGenerator: AddressIconGenerator,
        bittensorSubnetDetailsInteractor: BittensorSubnetDetailsInteractor,
        logger: Logger,
        validatorCommunicator: BittensorSelectValidatorCommunicator,
        fiatFormatter: FiatFormatter,
        chartAxisFormatterFactory: BittensorChartAxisFormatter.Factory
    ): ViewModel = BittensorSubnetDetailsViewModel(
        BittensorSubnetId(fragment.requireArguments().getInt("subnetId")),
        bittensorSubnetInteractor,
        bittensorSubnetStakingInteractor,
        router,
        resources,
        tokenFormatter,
        iconGenerator,
        bittensorSubnetDetailsInteractor,
        logger,
        validatorCommunicator,
        fiatFormatter,
        chartAxisFormatterFactory
    )

    @Provides
    @IntoMap
    @ViewModelKey(BittensorSelectValidatorViewModel::class)
    fun selectValidator(
        communicator: BittensorSelectValidatorCommunicator,
        bittensorSubnetStakingInteractor: BittensorSubnetStakingInteractor,
        icons: AddressIconGenerator,
        router: StakingRouter,
        externalActions: ExternalActions.Presentation,
        chainRegistry: ChainRegistry,
        resources: ResourceManager,
        logger: Logger
    ): ViewModel = BittensorSelectValidatorViewModel(
        communicator,
        bittensorSubnetStakingInteractor,
        icons,
        router,
        externalActions,
        chainRegistry,
        resources,
        logger
    )

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
