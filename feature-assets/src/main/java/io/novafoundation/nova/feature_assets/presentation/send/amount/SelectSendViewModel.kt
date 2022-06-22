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
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.WithFeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.requireFee
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
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
    Validatable by validationExecutor,
    WithFeeLoaderMixin {

    private val originChain by lazyAsync { chainRegistry.getChain(assetPayload.chainId) }
    private val chainAsset by lazyAsync { chainRegistry.asset(assetPayload.chainId, assetPayload.chainAssetId) }

    val addressInputMixin = addressInputMixinFactory.create(
        chainId = assetPayload.chainId,
        errorDisplayer = ::showError,
        coroutineScope = this
    )

    val originChainUi = flowOf {
        mapChainToUi(originChain())
    }
        .shareInBackground()

    private val destinationChain = singleReplaySharedFlow<Chain>()

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

    override val feeLoaderMixin: FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(commissionAssetFlow)

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

        listenFee()
    }

    fun nextClicked() = feeLoaderMixin.requireFee(this) { fee ->
        launch {
            val payload = AssetTransferPayload(
                transfer = buildTransfer(amountChooserMixin.amount.first(), addressInputMixin.inputFlow.first()),
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

    @OptIn(ExperimentalTime::class)
    private fun listenFee() {
        amountChooserMixin.backPressuredAmount
            .mapLatest(::loadFee)
            .launchIn(viewModelScope)
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

    private suspend fun loadFee(amount: BigDecimal) {
        feeLoaderMixin.loadFeeSuspending(
            retryScope = viewModelScope,
            feeConstructor = {
                sendInteractor.getTransferFee(buildTransfer(amount, addressInputMixin.inputFlow.first()))
            },
            onRetryCancelled = ::backClicked
        )
    }

    private suspend fun buildTransfer(amount: BigDecimal, address: String): AssetTransfer {
        val chain = originChain()

        return AssetTransfer(
            sender = selectedAccount.first(),
            recipient = address,
            chain = chain,
            chainAsset = chainAsset(),
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
