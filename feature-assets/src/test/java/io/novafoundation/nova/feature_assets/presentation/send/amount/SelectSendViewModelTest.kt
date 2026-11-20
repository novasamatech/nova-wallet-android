package io.novafoundation.nova.feature_assets.presentation.send.amount

import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.asPrecision
import io.novafoundation.nova.common.utils.asTokenSymbol
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixin
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressMixin
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_assets.domain.send.model.TransferFee
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.send.common.fee.TransferFeeDisplay
import io.novafoundation.nova.feature_wallet_api.data.repository.UsdRateRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.IncomingDirection
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixinBase.InputState
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.test_shared.any
import io.novafoundation.nova.test_shared.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import java.math.BigInteger

private const val ORIGIN_CHAIN_ID = "origin"
private const val DESTINATION_CHAIN_ID = "destination"
private const val INITIAL_STATE_TIMEOUT_MILLIS = 5_000L

@OptIn(ExperimentalCoroutinesApi::class)
class SelectSendViewModelTest {

    private val originAsset = chainAsset(ORIGIN_CHAIN_ID)

    private val destinationAsset = chainAsset(DESTINATION_CHAIN_ID)

    private val originChain = chain(ORIGIN_CHAIN_ID, originAsset)

    private val destinationChain = chain(DESTINATION_CHAIN_ID, destinationAsset)

    private val originToken = mock(Token::class.java)

    private val originBalance = mock(Asset::class.java)

    private val metaAccount = mock(MetaAccount::class.java)

    private val chainRegistry = mock(ChainRegistry::class.java)

    private val interactor = mock(WalletInteractor::class.java)

    private val sendInteractor = mock(SendInteractor::class.java)

    private val router = mock(AssetsRouter::class.java)

    private val validationExecutor = mock(ValidationExecutor::class.java)

    private val resourceManager = mock(ResourceManager::class.java) { invocation ->
        if (invocation.method.name == "getString") invocation.arguments.joinToString("|") else Mockito.RETURNS_DEFAULTS.answer(invocation)
    }

    private val externalActions = mock(ExternalActions.Presentation::class.java)

    private val crossChainTransfersUseCase = mock(CrossChainTransfersUseCase::class.java)

    private val accountRepository = mock(AccountRepository::class.java)

    private val maxActionProvider = mock(MaxActionProvider::class.java)

    private val maxActionProviderFactory = mock(MaxActionProviderFactory::class.java)

    private val analyticsService = mock(AnalyticsService::class.java)

    private val usdRateRepository = mock(UsdRateRepository::class.java)

    @Suppress("UNCHECKED_CAST")
    private val chooseDestinationChain = mock(ActionAwaitableMixin.Presentation::class.java) as ActionAwaitableMixin.Presentation<Any?, Any?>

    private val actionAwaitableMixinFactory = mock(ActionAwaitableMixin.Factory::class.java)

    @Suppress("UNCHECKED_CAST")
    private val feeMixin = mock(FeeLoaderMixinV2.Presentation::class.java) as FeeLoaderMixinV2.Presentation<TransferFee, TransferFeeDisplay>

    private val feeLoaderMixinFactory = mock(FeeLoaderMixinV2.Factory::class.java)

    private val selectedAccountUseCase = mock(SelectedAccountUseCase::class.java)

    private val addressInputMixin = mock(AddressInputMixin::class.java)

    private val addressInputMixinFactory = mock(AddressInputMixinFactory::class.java)

    private val amountChooserMixin = mock(AmountChooserMixin.Presentation::class.java)

    private val amountChooserMixinFactory = mock(AmountChooserMixin.Factory::class.java)

    private val selectAddressMixin = mock(SelectAddressMixin::class.java)

    private val selectAddressMixinFactory = mock(SelectAddressMixin.Factory::class.java)

    private val amountFormatter = mock(AmountFormatter::class.java)

    private val viewModels = mutableListOf<SelectSendViewModel>()

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        whenever(originToken.configuration).thenReturn(originAsset)
        whenever(originBalance.token).thenReturn(originToken)

        whenever(chainRegistry.chainsById).thenReturn(
            MutableStateFlow(mapOf(ORIGIN_CHAIN_ID to originChain, DESTINATION_CHAIN_ID to destinationChain))
        )
        whenever(interactor.assetFlow(any<Chain.Asset>())).thenReturn(emptyFlow())
        whenever(selectedAccountUseCase.selectedMetaAccountFlow()).thenReturn(flowOf(metaAccount))

        whenever(addressInputMixin.inputFlow).thenReturn(MutableStateFlow(""))
        whenever(addressInputMixinFactory.create(any(), any(), any(), any(), any(), any())).thenReturn(addressInputMixin)
        whenever(amountChooserMixin.inputState).thenReturn(MutableStateFlow(InputState.empty()))
        whenever(amountChooserMixin.backPressuredAmountState).thenReturn(emptyFlow())
        whenever(amountChooserMixinFactory.create(any(), any(), any(), any())).thenReturn(amountChooserMixin)
        whenever(selectAddressMixinFactory.create(any(), any(), any())).thenReturn(selectAddressMixin)
        whenever(actionAwaitableMixinFactory.create<Any?, Any?>()).thenReturn(chooseDestinationChain)
        whenever(feeLoaderMixinFactory.create<TransferFee, TransferFeeDisplay>(any(), any(), any(), any(), any())).thenReturn(feeMixin)
        whenever(
            maxActionProviderFactory.create<TransferFee>(any(), any(), any(), any<suspend (Asset) -> BigInteger>(), any())
        ).thenReturn(maxActionProvider)
    }

    @After
    fun teardown() = runBlocking {
        viewModels.forEach { it.coroutineContext.job.cancelAndJoin() }
        Dispatchers.resetMain()
    }

    @Test
    fun `specified destination resolves destination and origin when incoming directions are derived from the destination`() = runBlocking {
        // Like the real use case, directions are built per collector and only after that collector's destination is known.
        whenever(crossChainTransfersUseCase.incomingCrossChainDirections(any())).thenAnswer { invocation ->
            val destination = invocation.getArgument<Flow<Chain.Asset?>>(0)

            destination.map(::incomingDirectionsTo)
        }
        val viewModel = createViewModel(SendPayload.SpecifiedDestination(AssetPayload(DESTINATION_CHAIN_ID, destinationAsset.id)))

        val transferDirection = withTimeout(INITIAL_STATE_TIMEOUT_MILLIS) { viewModel.transferDirectionModel.first() }

        assertEquals(ORIGIN_CHAIN_ID, transferDirection.originChip.chainUi.id)
        assertEquals(DESTINATION_CHAIN_ID, transferDirection.destinationChip?.chainUi?.id)
    }

    @Test
    fun `specified origin without cross chain directions resolves an on chain transfer`() = runBlocking {
        whenever(crossChainTransfersUseCase.outcomingCrossChainDirectionsFlow(originAsset)).thenReturn(flowOf(emptyList()))
        val viewModel = createViewModel(SendPayload.SpecifiedOrigin(AssetPayload(ORIGIN_CHAIN_ID, originAsset.id)))

        val transferDirection = withTimeout(INITIAL_STATE_TIMEOUT_MILLIS) { viewModel.transferDirectionModel.first() }

        assertEquals(ORIGIN_CHAIN_ID, transferDirection.originChip.chainUi.id)
        assertNull(transferDirection.destinationChip)
    }

    private fun incomingDirectionsTo(destination: Chain.Asset?): List<IncomingDirection> {
        if (destination != destinationAsset) return emptyList()

        return listOf(IncomingDirection(originBalance, originChain))
    }

    private fun createViewModel(payload: SendPayload) = SelectSendViewModel(
        chainRegistry = chainRegistry,
        interactor = interactor,
        sendInteractor = sendInteractor,
        router = router,
        payload = payload,
        initialRecipientAddress = null,
        validationExecutor = validationExecutor,
        resourceManager = resourceManager,
        externalActions = externalActions,
        crossChainTransfersUseCase = crossChainTransfersUseCase,
        accountRepository = accountRepository,
        maxActionProviderFactory = maxActionProviderFactory,
        analyticsService = analyticsService,
        usdRateRepository = usdRateRepository,
        actionAwaitableMixinFactory = actionAwaitableMixinFactory,
        feeLoaderMixinFactory = feeLoaderMixinFactory,
        selectedAccountUseCase = selectedAccountUseCase,
        addressInputMixinFactory = addressInputMixinFactory,
        amountChooserMixinFactory = amountChooserMixinFactory,
        selectAddressMixinFactory = selectAddressMixinFactory,
        amountFormatter = amountFormatter
    ).also(viewModels::add)

    private fun chain(id: String, asset: Chain.Asset): Chain = mock(Chain::class.java).also {
        whenever(it.id).thenReturn(id)
        whenever(it.name).thenReturn(id)
        whenever(it.assetsById).thenReturn(mapOf(asset.id to asset))
        whenever(it.connectionState).thenReturn(Chain.ConnectionState.FULL_SYNC)
    }

    private fun chainAsset(chainId: String) = Chain.Asset(
        icon = null,
        id = 0,
        priceId = null,
        chainId = chainId,
        symbol = "DOT".asTokenSymbol(),
        precision = 10.asPrecision(),
        buyProviders = emptyMap(),
        sellProviders = emptyMap(),
        staking = emptyList(),
        type = Chain.Asset.Type.Native,
        source = Chain.Asset.Source.DEFAULT,
        name = "Polkadot"
    )
}
