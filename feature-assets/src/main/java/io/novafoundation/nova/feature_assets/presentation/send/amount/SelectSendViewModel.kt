package io.novafoundation.nova.feature_assets.presentation.send.amount

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.mapList
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
import io.novafoundation.nova.feature_account_api.view.ChainChipModel
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
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
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitDecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitOptionalDecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeToParcel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class SelectSendViewModel(
    private val chainRegistry: ChainRegistry,
    private val interactor: WalletInteractor,
    private val sendInteractor: SendInteractor,
    private val metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    private val router: AssetsRouter,
    private val payload: SendPayload,
    private val initialRecipientAddress: String?,
    private val validationExecutor: ValidationExecutor,
    private val resourceManager: ResourceManager,
    private val selectAddressRequester: SelectAddressForTransactionRequester,
    private val externalActions: ExternalActions.Presentation,
    private val crossChainTransfersUseCase: CrossChainTransfersUseCase,
    actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    selectedAccountUseCase: SelectedAccountUseCase,
    addressInputMixinFactory: AddressInputMixinFactory,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
) : BaseViewModel(),
    Validatable by validationExecutor,
    ExternalActions by externalActions {

    private val originChainWithAsset = singleReplaySharedFlow<ChainWithAsset>()
    private val destinationChainWithAsset = singleReplaySharedFlow<ChainWithAsset>()

    private val originAsset = originChainWithAsset.map { it.asset }
    private val originChain = originChainWithAsset.map { it.chain }

    private val destinationAsset = destinationChainWithAsset.map { it.asset }
    private val destinationChain = destinationChainWithAsset.map { it.chain }

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

    private val availableCrossChainDestinations = availableCrossChainDestinations()
        .onStart { emit(emptyList()) }
        .shareInBackground()

    val isSelectAddressAvailable = combine(originChain, destinationChain) { originChain, destinationChain ->
        metaAccountGroupingInteractor.hasAvailableMetaAccountsForDestination(originChain.id, destinationChain.id)
    }
        .shareInBackground()

    val transferDirectionModel = combine(
        availableCrossChainDestinations,
        originChainWithAsset,
        destinationChainWithAsset,
        ::buildTransferDirectionModel
    ).shareInBackground()

    val chooseDestinationChain = actionAwaitableMixinFactory.create<SelectCrossChainDestinationBottomSheet.Payload, ChainWithAsset>()

    private val selectedAccount = selectedAccountUseCase.selectedMetaAccountFlow()
        .inBackground()
        .share()

    private val sendInProgressFlow = MutableStateFlow(false)

    private val originAssetFlow = originAsset.flatMapLatest(interactor::assetFlow)
        .shareInBackground()

    private val commissionAssetFlow = originChain.flatMapLatest(interactor::commissionAssetFlow)
        .shareInBackground()

    val originFeeMixin: FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(commissionAssetFlow)
    val crossChainFeeMixin: FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(originAssetFlow)

    val amountChooserMixin: AmountChooserMixin.Presentation = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = originAssetFlow,
        balanceLabel = R.string.wallet_balance_transferable,
        balanceField = Asset::transferable,
    )

    val continueButtonStateLiveData = combine(
        sendInProgressFlow,
        amountChooserMixin.inputState
    ) { sending, amountState ->
        when {
            sending -> ButtonState.PROGRESS
            amountState.value.isNotEmpty() -> ButtonState.NORMAL
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
            amount = amountChooserMixin.amountState.first().value ?: return@launch,
            address = addressInputMixin.getAddress(),
        )

        val payload = AssetTransferPayload(
            transfer = WeightedAssetTransfer(
                assetTransfer = transfer,
                fee = originFee,
            ),
            crossChainFee = crossChainFee?.networkFeeDecimalAmount,
            originFee = originFee.networkFeeDecimalAmount,
            originCommissionAsset = commissionAssetFlow.first(),
            originUsedAsset = originAssetFlow.first()
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
            val currentOriginChain = originChain.first()
            val currentDestinationChain = destinationChain.first()
            val request = SelectAddressForTransactionRequester.Request(currentOriginChain.id, currentDestinationChain.id, selectedAddress)
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

        when (payload) {
            is SendPayload.SpecifiedOrigin -> {
                val origin = chainRegistry.chainWithAsset(payload.origin.chainId, payload.origin.chainAssetId)
                originChainWithAsset.emit(origin)
                destinationChainWithAsset.emit(origin)
            }
            is SendPayload.SpecifiedDestination -> {
                val destination = chainRegistry.chainWithAsset(payload.destination.chainId, payload.destination.chainAssetId)
                val origin = availableCrossChainDestinations.first().first().chainWithAsset
                destinationChainWithAsset.emit(destination)
                originChainWithAsset.emit(origin)
            }
        }
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
            origin = AssetPayload(
                chainId = validPayload.transfer.originChain.id,
                chainAssetId = validPayload.transfer.originChainAsset.id
            ),
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
        val origin = originChainWithAsset.first()

        return BaseAssetTransfer(
            sender = selectedAccount.first(),
            recipient = address,
            originChain = origin.chain,
            originChainAsset = origin.asset,
            destinationChain = destination.chain,
            destinationChainAsset = destination.asset,
            amount = amount,
            commissionAssetToken = commissionAssetFlow.first().token,
        )
    }

    private fun buildTransferDirectionModel(
        availableCrossChainDestinations: List<CrossChainDirection>,
        origin: ChainWithAsset,
        destination: ChainWithAsset
    ): TransferDirectionModel {
        return when (payload) {
            is SendPayload.SpecifiedDestination -> buildInTransferDirectionModel(availableCrossChainDestinations, origin, destination)
            is SendPayload.SpecifiedOrigin ->  buildOutTransferDirectionModel(availableCrossChainDestinations, origin, destination)
        }
    }

    private fun buildInTransferDirectionModel(
        availableCrossChainDestinations: List<CrossChainDirection>,
        origin: ChainWithAsset,
        destination: ChainWithAsset
    ): TransferDirectionModel {
        val chainSymbol = origin.asset.symbol
        val destinationChip = ChainChipModel(
            chainUi = mapChainToUi(destination.chain),
            changeable = false // when in is specified destination is never changeable
        )
        val originLabel = resourceManager.getString(R.string.wallet_send_tokens_from, chainSymbol)

        return if (availableCrossChainDestinations.size > 1) {
            TransferDirectionModel(
                originChip = ChainChipModel(
                    chainUi = mapChainToUi(origin.chain),
                    changeable = true
                ),
                originChainLabel = originLabel,
                destinationChip = destinationChip
            )
        } else {
            TransferDirectionModel(
                originChip = ChainChipModel(
                    chainUi = mapChainToUi(origin.chain),
                    changeable = false
                ),
                originChainLabel = originLabel,
                destinationChip = destinationChip
            )
        }
    }

    private fun buildOutTransferDirectionModel(
        availableCrossChainDestinations: List<CrossChainDirection>,
        origin: ChainWithAsset,
        destination: ChainWithAsset
    ): TransferDirectionModel {
        val chainSymbol = origin.asset.symbol
        val originChip = ChainChipModel(
            chainUi = mapChainToUi(origin.chain),
            changeable = false // when out is specified origin is never changeable
        )

        return if (availableCrossChainDestinations.isNotEmpty()) {
            TransferDirectionModel(
                originChip = originChip,
                originChainLabel = resourceManager.getString(R.string.wallet_send_tokens_from, chainSymbol),
                destinationChip = ChainChipModel(
                    chainUi = mapChainToUi(destination.chain),
                    changeable = true // we can always change between at least one cross chain transfer and on-chain one
                )
            )
        } else {
            TransferDirectionModel(
                originChip = originChip,
                originChainLabel = resourceManager.getString(R.string.wallet_send_tokens_on, chainSymbol),
                destinationChip = null
            )
        }
    }

    private suspend fun buildDestinationsMap(crossChainDestinations: List<CrossChainDirection>): Map<TextHeader, List<CrossChainDestinationModel>> {
        val crossChainDestinationModels = crossChainDestinations.map {
            CrossChainDestinationModel(
                chainWithAsset = it.chainWithAsset,
                chainUi = mapChainToUi(it.chainWithAsset.chain),
                balance = it.balances?.let { asset -> mapAmountToAmountModel(asset.transferable, asset) }
            )
        }

        val onChainDestinationModel = if (payload is SendPayload.SpecifiedOrigin) {
            val origin = originChainWithAsset.first()

            CrossChainDestinationModel(
                chainWithAsset = origin,
                chainUi = mapChainToUi(origin.chain),
                balance = null
            )
        } else {
            null
        }

        return buildMap {
            onChainDestinationModel?.let {
                put(TextHeader(resourceManager.getString(R.string.wallet_send_on_chain)), listOf(onChainDestinationModel))
            }

            put(TextHeader(resourceManager.getString(R.string.wallet_send_cross_chain)), crossChainDestinationModels)
        }
    }

    private fun availableCrossChainDestinations(): Flow<List<CrossChainDirection>> {
        return when (payload) {
            is SendPayload.SpecifiedDestination -> availableInDirections()
            is SendPayload.SpecifiedOrigin -> availableOutDirections()
        }
    }

    private fun availableInDirections(): Flow<List<CrossChainDirection>> {
        return crossChainTransfersUseCase.incomingCrossChainDirections(destinationAsset).mapList { incomingDirection ->
            CrossChainDirection(
                chainWithAsset = ChainWithAsset(incomingDirection.chain, incomingDirection.asset.token.configuration),
                balances = incomingDirection.asset
            )
        }
    }

    private fun availableOutDirections(): Flow<List<CrossChainDirection>> {
        return originAsset.flatMapLatest {
            crossChainTransfersUseCase.outcomingCrossChainDirections(it).mapList { incomingDirection ->
                CrossChainDirection(
                    chainWithAsset = ChainWithAsset(incomingDirection.chain, incomingDirection.asset),
                    balances = null
                )
            }
        }
    }

    private class CrossChainDirection(
        val chainWithAsset: ChainWithAsset,
        val balances: Asset?
    )
}
