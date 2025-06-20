package io.novafoundation.nova.feature_assets.presentation.send.confirm

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.memory.getOrThrow
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_api.presenatation.fee.toDomain
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_assets.domain.send.TransferFeeScopedStore
import io.novafoundation.nova.feature_assets.domain.send.model.transferFee
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.send.TransferDraft
import io.novafoundation.nova.feature_assets.presentation.send.autoFixSendValidationPayload
import io.novafoundation.nova.feature_assets.presentation.send.common.fee.TransferFeeDisplayFormatter
import io.novafoundation.nova.feature_assets.presentation.send.common.fee.createForTransfer
import io.novafoundation.nova.feature_assets.presentation.send.confirm.hints.ConfirmSendHintsMixinFactory
import io.novafoundation.nova.feature_assets.presentation.send.isCrossChain
import io.novafoundation.nova.feature_assets.presentation.send.mapAssetTransferValidationFailureToUI
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.WeightedAssetTransfer
import io.novafoundation.nova.feature_wallet_api.domain.model.OriginFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2.Configuration
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountSign
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

class ConfirmSendChainsModel(
    val origin: ChainUi,
    val originChainLabel: String,
    val destination: ChainUi?
)

class ConfirmSendViewModel(
    private val interactor: WalletInteractor,
    private val sendInteractor: SendInteractor,
    private val router: AssetsRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val externalActions: ExternalActions.Presentation,
    private val chainRegistry: ChainRegistry,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val walletUiUseCase: WalletUiUseCase,
    private val hintsFactory: ConfirmSendHintsMixinFactory,
    private val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
    private val transferFeeScopedStore: TransferFeeScopedStore,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    val transferDraft: TransferDraft,
) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor,
    ExtrinsicNavigationWrapper by extrinsicNavigationWrapper {

    private val isCrossChain = transferDraft.origin.chainId != transferDraft.destination.chainId

    private val originChain by lazyAsync { chainRegistry.getChain(transferDraft.origin.chainId) }
    private val originAsset by lazyAsync { chainRegistry.asset(transferDraft.origin.chainId, transferDraft.origin.chainAssetId) }

    private val destinationChain by lazyAsync { chainRegistry.getChain(transferDraft.destination.chainId) }
    private val destinationChainAsset by lazyAsync { chainRegistry.asset(transferDraft.destination.chainId, transferDraft.destination.chainAssetId) }

    private val assetFlow = interactor.assetFlow(transferDraft.origin.chainId, transferDraft.origin.chainAssetId)
        .inBackground()
        .share()

    private val currentAccount = selectedAccountUseCase.selectedMetaAccountFlow()
        .inBackground()
        .share()

    private val formatter = TransferFeeDisplayFormatter(crossChainFeeShown = isCrossChain)
    val feeMixin = feeLoaderMixinFactory.createForTransfer(
        originChainAsset = flowOf { originAsset() },
        formatter = formatter,
        configuration = Configuration(
            initialState = Configuration.InitialState(
                feePaymentCurrency = transferDraft.feePaymentCurrency.toDomain()
            )
        )
    )

    val hintsMixin = hintsFactory.create(this)

    val recipientModel = flowOf {
        createAddressModel(
            address = transferDraft.recipientAddress,
            chain = destinationChain(),
            resolveName = true
        )
    }
        .inBackground()
        .share()

    val senderModel = currentAccount.mapLatest { metaAccount ->
        createAddressModel(
            address = metaAccount.requireAddressIn(originChain()),
            chain = originChain(),
            resolveName = false
        )
    }
        .inBackground()
        .share()

    val amountModel = assetFlow.map { asset ->
        mapAmountToAmountModel(transferDraft.amount, asset, tokenAmountSign = AmountSign.NEGATIVE)
    }

    val transferDirectionModel = flowOf { createTransferDirectionModel() }
        .shareInBackground()

    val wallet = walletUiUseCase.selectedWalletUiFlow()
        .inBackground()
        .share()

    private val _transferSubmittingLiveData = MutableStateFlow(false)

    val sendButtonStateLiveData = _transferSubmittingLiveData.map { submitting ->
        if (submitting) {
            ButtonState.PROGRESS
        } else {
            ButtonState.NORMAL
        }
    }

    init {
        setupFee()
    }

    fun backClicked() {
        router.back()
    }

    fun recipientAddressClicked() = launch {
        showExternalActions(transferDraft.recipientAddress, destinationChain)
    }

    fun senderAddressClicked() = launch {
        showExternalActions(senderModel.first().address, originChain)
    }

    private suspend fun showExternalActions(
        address: String,
        chain: Deferred<Chain>,
    ) {
        externalActions.showAddressActions(address, chain())
    }

    fun submitClicked() = launch {
        val payload = buildValidationPayload()

        validationExecutor.requireValid(
            validationSystem = sendInteractor.validationSystemFor(payload.transfer, viewModelScope),
            payload = payload,
            progressConsumer = _transferSubmittingLiveData.progressConsumer(),
            autoFixPayload = ::autoFixSendValidationPayload,
            validationFailureTransformerCustom = { status, actions ->
                viewModelScope.mapAssetTransferValidationFailureToUI(
                    resourceManager = resourceManager,
                    status = status,
                    actions = actions,
                    setFee = {
                        val newOriginFee = payload.transferFee().replaceSubmission(it)
                        feeMixin.setFee(newOriginFee)
                    }
                )
            },
        ) { validPayload ->
            performTransfer(validPayload.transfer, validPayload.originFee, validPayload.crossChainFee)
        }
    }

    private fun setupFee() = launchUnit{
        val fee = transferFeeScopedStore.getOrThrow()
        feeMixin.setFee(fee)
    }

    private suspend fun createAddressModel(
        address: String,
        chain: Chain,
        resolveName: Boolean
    ) =
        addressIconGenerator.createAddressModel(
            chain = chain,
            sizeInDp = AddressIconGenerator.SIZE_MEDIUM,
            address = address,
            background = AddressIconGenerator.BACKGROUND_TRANSPARENT,
            addressDisplayUseCase = addressDisplayUseCase.takeIf { resolveName }
        )

    private fun performTransfer(
        transfer: WeightedAssetTransfer,
        originFee: OriginFee,
        crossChainFee: FeeBase?
    ) = launch {
        sendInteractor.performTransfer(transfer, originFee, crossChainFee, viewModelScope)
            .onSuccess {
                showMessage(resourceManager.getString(R.string.common_transaction_submitted))

                startNavigation(it.submissionHierarchy) { finishSendFlow() }
            }.onFailure(::showError)

        _transferSubmittingLiveData.value = false
    }

    private fun finishSendFlow() = launch {
        val chain = originChain()
        val chainAsset = originAsset()

        if (transferDraft.openAssetDetailsOnCompletion) {
            router.openAssetDetails(AssetPayload(chain.id, chainAsset.id))
        } else {
            router.closeSendFlow()
        }
    }

    private suspend fun buildValidationPayload(): AssetTransferPayload {
        val chain = originChain()
        val chainAsset = originAsset()

        val fee = feeMixin.awaitFee()

        return AssetTransferPayload(
            transfer = WeightedAssetTransfer(
                sender = currentAccount.first(),
                recipient = transferDraft.recipientAddress,
                originChain = chain,
                destinationChain = destinationChain(),
                destinationChainAsset = destinationChainAsset(),
                originChainAsset = chainAsset,
                amount = transferDraft.amount,
                feePaymentCurrency = transferDraft.feePaymentCurrency.toDomain(),
                fee = fee.originFee,
                transferringMaxAmount = transferDraft.transferringMaxAmount
            ),
            originCommissionAsset = feeMixin.feeAsset(),
            originUsedAsset = assetFlow.first(),
            crossChainFee = fee.crossChainFee
        )
    }

    private suspend fun createTransferDirectionModel() = if (transferDraft.isCrossChain) {
        ConfirmSendChainsModel(
            origin = mapChainToUi(originChain()),
            originChainLabel = resourceManager.getString(R.string.wallet_send_from_network),
            destination = mapChainToUi(destinationChain())
        )
    } else {
        ConfirmSendChainsModel(
            origin = mapChainToUi(originChain()),
            originChainLabel = resourceManager.getString(R.string.common_network),
            destination = null
        )
    }
}
