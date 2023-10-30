package io.novafoundation.nova.feature_assets.presentation.send.amount

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectAddressForTransactionRequester
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.send.TransferDirectionModel
import io.novafoundation.nova.feature_assets.presentation.send.TransferDraft
import io.novafoundation.nova.feature_assets.presentation.send.amount.view.CrossChainDestinationModel
import io.novafoundation.nova.feature_assets.presentation.send.amount.view.SelectCrossChainDestinationBottomSheet
import io.novafoundation.nova.feature_assets.presentation.send.autoFixSendValidationPayload
import io.novafoundation.nova.feature_assets.presentation.send.mapAssetTransferValidationFailureToUI
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.BaseAssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.WeightedAssetTransfer
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitDecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitOptionalDecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeToParcel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.asset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class SelectSendViewModel(
    private val interactor: WalletInteractor,
    private val sendInteractor: SendInteractor,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val router: AssetsRouter,
    private val assetPayload: AssetPayload,
    private val initialRecipientAddress: String?,
    private val chainRegistry: ChainRegistry,
    private val validationExecutor: ValidationExecutor,
    private val feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val resourceManager: ResourceManager,
    private val addressInputMixinFactory: AddressInputMixinFactory,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
    private val selectAddressRequester: SelectAddressForTransactionRequester,
    private val externalActions: ExternalActions.Presentation
) : BaseViewModel(),
    Validatable by validationExecutor,
    ExternalActions by externalActions {

    private val originChain by lazyAsync { chainRegistry.getChain(assetPayload.chainId) }
    private val originChainAsset by lazyAsync { chainRegistry.asset(assetPayload.chainId, assetPayload.chainAssetId) }

    private val destinationChainWithAsset = singleReplaySharedFlow<ChainWithAsset>()

    val addressInputMixin = with(addressInputMixinFactory) {
        val destinationChain = destinationChainWithAsset.map { it.chain }
        val inputSpec = singleChainInputSpec(destinationChain)

        create(
            inputSpecProvider = singleChainInputSpec(destinationChain),
            myselfBehaviorProvider = crossChainOnlyMyself(originChain, destinationChain),
            accountIdentifierProvider = web3nIdentifiers(
                destinationChainFlow = destinationChainWithAsset,
                inputSpecProvider = inputSpec,
                coroutineScope = this@SelectSendViewModel,
            ),
            errorDisplayer = this@SelectSendViewModel::showError,
            showAccountEvent = this@SelectSendViewModel::showAccountDetails,
            coroutineScope = this@SelectSendViewModel,
        )
    }

    private val availableCrossChainDestinations = flow {
        val origin = originChainAsset()

        emitAll(sendInteractor.availableCrossChainDestinationsFlow(origin))
    }
        .onStart { emit(emptyList()) }
        .shareInBackground()

    val isSelectAddressAvailable = destinationChainWithAsset
        .map { metaAccountGroupingInteractor.hasAvailableMetaAccountsForDestination(assetPayload.chainId, it.chain.id) }
        .inBackground()
        .share()

    val transferDirectionModel = combine(
        availableCrossChainDestinations,
        destinationChainWithAsset,
        ::buildTransferDirectionModel
    ).shareInBackground()

    val chooseDestinationChain = actionAwaitableMixinFactory.create<SelectCrossChainDestinationBottomSheet.Payload, ChainWithAsset>()

    private val selectedAccount = selectedAccountUseCase.selectedMetaAccountFlow()
        .inBackground()
        .share()

    private val sendInProgressFlow = MutableStateFlow(false)

    private val assetFlow = interactor.assetFlow(assetPayload.chainId, assetPayload.chainAssetId)
        .inBackground()
        .share()

    private val commissionAssetFlow = interactor.commissionAssetFlow(assetPayload.chainId)
        .inBackground()
        .share()

    val originFeeMixin: FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(commissionAssetFlow)
    val crossChainFeeMixin: FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(assetFlow)

    val amountChooserMixin: AmountChooserMixin.Presentation = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = assetFlow,
        balanceLabel = R.string.wallet_balance_transferable,
        balanceField = Asset::transferable,
    )

    val continueButtonStateLiveData = combine(
        sendInProgressFlow,
        amountChooserMixin.amountInput
    ) { sending, amountRaw ->
        when {
            sending -> ButtonState.PROGRESS
            amountRaw.isNotEmpty() -> ButtonState.NORMAL
            else -> ButtonState.DISABLED
        }
    }

    init {
        subscribeOnChangeDestination()

        subscribeOnSelectAddress()

        setInitialState()

        setupFees()

        syncCrossChainConfig()
    }

    fun nextClicked() = launch {
        sendInProgressFlow.value = true

        val originFee = originFeeMixin.awaitDecimalFee()
        val crossChainFee = crossChainFeeMixin.awaitOptionalDecimalFee()

        val transfer = buildTransfer(
            destination = destinationChainWithAsset.first(),
            amount = amountChooserMixin.amount.first(),
            address = addressInputMixin.getAddress(),
        )

        val payload = AssetTransferPayload(
            transfer = WeightedAssetTransfer(
                assetTransfer = transfer,
                fee = originFee,
            ),
            crossChainFee = crossChainFee?.decimalAmount,
            originFee = originFee.decimalAmount,
            originCommissionAsset = commissionAssetFlow.first(),
            originUsedAsset = assetFlow.first()
        )

        validationExecutor.requireValid(
            validationSystem = sendInteractor.validationSystemFor(payload.transfer),
            payload = payload,
            progressConsumer = sendInProgressFlow.progressConsumer(),
            autoFixPayload = ::autoFixSendValidationPayload,
            validationFailureTransformerCustom = { status, actions ->
                viewModelScope.mapAssetTransferValidationFailureToUI(
                    resourceManager = resourceManager,
                    status = status,
                    actions = actions,
                    feeLoaderMixin = originFeeMixin
                )
            },
        ) {
            sendInProgressFlow.value = false

            openConfirmScreen(it)
        }
    }

    fun backClicked() {
        router.back()
    }

    fun destinationChainClicked() = launch {
        val destinations = availableCrossChainDestinations.first()
        if (destinations.isEmpty()) return@launch

        val payload = withContext(Dispatchers.Default) {
            SelectCrossChainDestinationBottomSheet.Payload(
                destinations = buildDestinationsMap(destinations),
                selectedChain = destinationChainWithAsset.first().chain
            )
        }

        val newDestinationChain = chooseDestinationChain.awaitAction(payload)

        destinationChainWithAsset.emit(newDestinationChain)
    }

    fun selectRecipientWallet() {
        launch {
            val selectedAddress = addressInputMixin.inputFlow.value
            val currentDestination = destinationChainWithAsset.first().chain
            val request = SelectAddressForTransactionRequester.Request(assetPayload.chainId, currentDestination.id, selectedAddress)
            selectAddressRequester.openRequest(request)
        }
    }

    private fun showAccountDetails(address: String) {
        launch {
            val chain = destinationChainWithAsset.first().chain
            externalActions.showExternalActions(ExternalActions.Type.Address(address), chain)
        }
    }

    private fun subscribeOnChangeDestination() {
        destinationChainWithAsset
            .onEach { addressInputMixin.clearExtendedAccount() }
            .launchIn(this)
    }

    private fun subscribeOnSelectAddress() {
        selectAddressRequester.responseFlow
            .onEach {
                addressInputMixin.inputFlow.value = it.selectedAddress
            }
            .launchIn(this)
    }

    private fun setInitialState() = launch {
        initialRecipientAddress?.let { addressInputMixin.inputFlow.value = it }

        destinationChainWithAsset.emit(ChainWithAsset(originChain(), originChainAsset()))
    }

    private fun syncCrossChainConfig() = launch {
        sendInteractor.syncCrossChainConfig()
    }

    private fun setupFees() {
        originFeeMixin.setupFee { transfer -> sendInteractor.getOriginFee(transfer) }
        crossChainFeeMixin.setupFee { transfer -> sendInteractor.getCrossChainFee(transfer) }
    }

    private fun FeeLoaderMixin.Presentation.setupFee(
        feeConstructor: suspend Token.(transfer: AssetTransfer) -> Fee?
    ) {
        connectWith(
            inputSource1 = amountChooserMixin.backPressuredAmount,
            inputSource2 = destinationChainWithAsset,
            inputSource3 = addressInputMixin.inputFlow,
            scope = viewModelScope,
            feeConstructor = { amount, destinationChain, addressInput ->
                val transfer = buildTransfer(destinationChain, amount, addressInput)

                feeConstructor(transfer)
            }
        )
    }

    private fun openConfirmScreen(validPayload: AssetTransferPayload) = launch {
        val transferDraft = TransferDraft(
            amount = validPayload.transfer.amount,
            originFee = mapFeeToParcel(validPayload.transfer.decimalFee),
            origin = assetPayload,
            destination = AssetPayload(
                chainId = validPayload.transfer.destinationChain.id,
                chainAssetId = validPayload.transfer.destinationChainAsset.id
            ),
            recipientAddress = validPayload.transfer.recipient,
            crossChainFee = validPayload.crossChainFee
        )

        router.openConfirmTransfer(transferDraft)
    }

    private suspend fun buildTransfer(
        destination: ChainWithAsset,
        amount: BigDecimal,
        address: String,
    ): AssetTransfer {
        return BaseAssetTransfer(
            sender = selectedAccount.first(),
            recipient = address,
            originChain = originChain(),
            originChainAsset = originChainAsset(),
            destinationChain = destination.chain,
            destinationChainAsset = destination.asset,
            amount = amount,
            commissionAssetToken = commissionAssetFlow.first().token,
        )
    }

    private suspend fun buildTransferDirectionModel(
        availableCrossChainDestinations: List<ChainWithAsset>,
        destinationChain: ChainWithAsset
    ): TransferDirectionModel {
        val chainSymbol = originChainAsset().symbol

        return if (availableCrossChainDestinations.isEmpty()) {
            TransferDirectionModel(
                originChainUi = mapChainToUi(originChain()),
                originChainLabel = resourceManager.getString(R.string.wallet_send_tokens_on, chainSymbol),
                destinationChainUi = null
            )
        } else {
            TransferDirectionModel(
                originChainUi = mapChainToUi(originChain()),
                originChainLabel = resourceManager.getString(R.string.wallet_send_tokens_from, chainSymbol),
                destinationChainUi = mapChainToUi(destinationChain.chain)
            )
        }
    }

    private suspend fun buildDestinationsMap(crossChainDestinations: List<ChainWithAsset>): Map<TextHeader, List<CrossChainDestinationModel>> {
        val crossChainDestinationModels = crossChainDestinations.map {
            CrossChainDestinationModel(
                chainWithAsset = it,
                chainUi = mapChainToUi(it.chain)
            )
        }
        val onChainDestination = CrossChainDestinationModel(
            chainWithAsset = ChainWithAsset(originChain(), originChainAsset()),
            chainUi = transferDirectionModel.first().originChainUi
        )

        return buildMap {
            put(
                TextHeader(resourceManager.getString(R.string.wallet_send_on_chain)),
                listOf(onChainDestination)
            )

            put(
                TextHeader(resourceManager.getString(R.string.wallet_send_cross_chain)),
                crossChainDestinationModels
            )
        }
    }
}
