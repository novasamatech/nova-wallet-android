package io.novafoundation.nova.feature_assets.presentation.send.amount

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_account_api.view.ChainChipModel
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.WalletRouter
import io.novafoundation.nova.feature_assets.presentation.send.TransferDraft
import io.novafoundation.nova.feature_assets.presentation.send.amount.view.CrossChainDestinationModel
import io.novafoundation.nova.feature_assets.presentation.send.amount.view.SelectCrossChainDestinationBottomSheet
import io.novafoundation.nova.feature_assets.presentation.send.mapAssetTransferValidationFailureToUI
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure.WillRemoveAccount
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.requireFee
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.time.ExperimentalTime

class SelectSendViewModel(
    private val interactor: WalletInteractor,
    private val sendInteractor: SendInteractor,
    private val router: WalletRouter,
    private val assetPayload: AssetPayload,
    private val initialRecipientAddress: String?,
    private val chainRegistry: ChainRegistry,
    private val validationExecutor: ValidationExecutor,
    private val feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val resourceManager: ResourceManager,
    private val addressInputMixinFactory: AddressInputMixinFactory,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    amountChooserMixinFactory: AmountChooserMixin.Factory
) : BaseViewModel(),
    Validatable by validationExecutor {

    private val originChain by lazyAsync { chainRegistry.getChain(assetPayload.chainId) }
    private val chainAsset by lazyAsync { chainRegistry.asset(assetPayload.chainId, assetPayload.chainAssetId) }

    val originChainUi = flowOf {
        mapChainToUi(originChain())
    }
        .shareInBackground()

    private val destinationChain = singleReplaySharedFlow<Chain>()

    val addressInputMixin = addressInputMixinFactory.create(
        chainFlow = destinationChain,
        errorDisplayer = ::showError,
        coroutineScope = this
    )

    private val availableCrossChainDestinations = flowOf {
        sendInteractor.availableCrossChainDestinations(chainAsset())
    }
        .onStart { emit(emptyList()) }
        .shareInBackground()

    val destinationChainChipModel = combine(
        availableCrossChainDestinations,
        destinationChain
    ) { availableDestinations, currentDestination ->
        ChainChipModel(
            chainUi = mapChainToUi(currentDestination),
            changeable = availableDestinations.isNotEmpty()
        )
    }

    val chooseDestinationChain = actionAwaitableMixinFactory.create<SelectCrossChainDestinationBottomSheet.Payload, Chain>()

    val sendFromText = flowOf {
        resourceManager.getString(R.string.wallet_send_tokens_on, chainAsset().symbol)
    }
        .inBackground()
        .share()

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
        setInitialState()

        setupFees()

        syncCrossChainConfig()
    }

    fun nextClicked() = originFeeMixin.requireFee(this) { fee ->
        launch {
            val payload = AssetTransferPayload(
                transfer = buildTransfer(
                    destinationChain = destinationChain.first(),
                    amount = amountChooserMixin.amount.first(),
                    address = addressInputMixin.inputFlow.first()
                ),
                fee = fee,
                commissionAsset = commissionAssetFlow.first(),
                usedAsset = assetFlow.first()
            )

            validationExecutor.requireValid(
                validationSystem = sendInteractor.validationSystemFor(chainAsset()),
                payload = payload,
                progressConsumer = sendInProgressFlow.progressConsumer(),
                autoFixPayload = ::autoFixValidationPayload,
                validationFailureTransformer = { mapAssetTransferValidationFailureToUI(resourceManager, it) }
            ) {
                sendInProgressFlow.value = false

                openConfirmScreen(it)
            }
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
                selectedChain = destinationChain.first()
            )
        }

        val newDestinationChain = chooseDestinationChain.awaitAction(payload)

        destinationChain.emit(newDestinationChain)
    }

    private fun setInitialState()  = launch {
        initialRecipientAddress?.let { addressInputMixin.inputFlow.value = it }

        destinationChain.emit(originChain())
    }

    private fun syncCrossChainConfig() = launch {
        sendInteractor.syncCrossChainConfig()
    }

    @OptIn(ExperimentalTime::class)
    private fun setupFees() {
        originFeeMixin.setupFee { transfer -> sendInteractor.getOriginFee(transfer) }
        crossChainFeeMixin.setupFee { transfer -> sendInteractor.getCrossChainFee(transfer) }
    }

    private fun FeeLoaderMixin.Presentation.setupFee(
        feeConstructor: suspend Token.(transfer: AssetTransfer) -> BigInteger?
    ) {
        connectWith(
            inputSource1 = amountChooserMixin.backPressuredAmount,
            inputSource2 = destinationChain,
            scope = viewModelScope,
            feeConstructor = { amount, destinationChain ->
                val transfer = buildTransfer(destinationChain, amount, addressInputMixin.inputFlow.first())

                feeConstructor(transfer)
            }
        )
    }

    private fun openConfirmScreen(validPayload: AssetTransferPayload) = launch {
        val transferDraft = TransferDraft(
            amount = validPayload.transfer.amount,
            fee = validPayload.fee,
            assetPayload = assetPayload,
            recipientAddress = validPayload.transfer.recipient
        )

        router.openConfirmTransfer(transferDraft)
    }

    private fun autoFixValidationPayload(
        payload: AssetTransferPayload,
        failureReason: AssetTransferValidationFailure
    ) = when (failureReason) {
        is WillRemoveAccount.WillTransferDust -> payload.copy(
            transfer = payload.transfer.copy(
                amount = payload.transfer.amount + failureReason.dust
            )
        )
        else -> payload
    }

    private suspend fun buildTransfer(destinationChain: Chain, amount: BigDecimal, address: String): AssetTransfer {
        return AssetTransfer(
            sender = selectedAccount.first(),
            recipient = address,
            originChain = originChain(),
            originChainAsset = chainAsset(),
            destinationChain = destinationChain,
            amount = amount
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun buildDestinationsMap(crossChainDestinations: List<Chain>): Map<TextHeader, List<CrossChainDestinationModel>> {
        val crossChainDestinationModels = crossChainDestinations.map {
            CrossChainDestinationModel(
                chain = it,
                chainUi = mapChainToUi(it)
            )
        }
        val onChainDestination = CrossChainDestinationModel(
            chain = originChain(),
            chainUi = originChainUi.first()
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
