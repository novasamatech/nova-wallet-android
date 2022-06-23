package io.novafoundation.nova.feature_assets.presentation.send.confirm

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_assets.presentation.WalletRouter
import io.novafoundation.nova.feature_assets.presentation.send.TransferDraft
import io.novafoundation.nova.feature_assets.presentation.send.confirm.hints.ConfirmSendHintsMixinFactory
import io.novafoundation.nova.feature_assets.presentation.send.confirm.model.TransferDirectionModel
import io.novafoundation.nova.feature_assets.presentation.send.isCrossChain
import io.novafoundation.nova.feature_assets.presentation.send.mapAssetTransferValidationFailureToUI
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountSign
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ConfirmSendViewModel(
    private val interactor: WalletInteractor,
    private val sendInteractor: SendInteractor,
    private val router: WalletRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val externalActions: ExternalActions.Presentation,
    private val chainRegistry: ChainRegistry,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val walletUiUseCase: WalletUiUseCase,
    private val hintsFactory: ConfirmSendHintsMixinFactory,
    feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    val transferDraft: TransferDraft,
) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor {

    private val originChain by lazyAsync { chainRegistry.getChain(transferDraft.assetPayload.chainId) }
    private val originAsset by lazyAsync { chainRegistry.asset(transferDraft.assetPayload.chainId, transferDraft.assetPayload.chainAssetId) }

    private val destinationChain by lazyAsync { chainRegistry.getChain(transferDraft.destinationChain) }

    private val assetFlow = interactor.assetFlow(transferDraft.assetPayload.chainId, transferDraft.assetPayload.chainAssetId)
        .inBackground()
        .share()

    private val commissionAssetFlow = interactor.commissionAssetFlow(transferDraft.assetPayload.chainId)
        .inBackground()
        .share()

    val originFeeMixin: FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(commissionAssetFlow)
    val crossChainFeeMixin: FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(assetFlow)

    val hintsMixin = hintsFactory.create(this)

    private val currentAccount = selectedAccountUseCase.selectedMetaAccountFlow()
        .inBackground()
        .share()

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
        setInitialState()
    }

    fun backClicked() {
        router.back()
    }

    fun recipientAddressClicked() = launch {
        showExternalActions(transferDraft.recipientAddress)
    }

    fun senderAddressClicked() = launch {
        showExternalActions(senderModel.first().address)
    }

    private suspend fun showExternalActions(address: String) {
        externalActions.showExternalActions(ExternalActions.Type.Address(address), originChain())
    }

    fun submitClicked() = launch {
        val payload = buildValidationPayload()

        validationExecutor.requireValid(
            validationSystem = sendInteractor.validationSystemFor(payload.transfer),
            payload = payload,
            progressConsumer = _transferSubmittingLiveData.progressConsumer(),
            validationFailureTransformer = { mapAssetTransferValidationFailureToUI(resourceManager, it) }
        ) { validPayload ->
            performTransfer(validPayload.transfer, validPayload.originFee, validPayload.crossChainFee)
        }
    }

    private fun setInitialState() = launch {
        originFeeMixin.setFee(transferDraft.originFee)
        crossChainFeeMixin.setFee(transferDraft.crossChainFee)
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
        transfer: AssetTransfer,
        originFee: BigDecimal,
        crossChainFee: BigDecimal?
    ) = launch {
        sendInteractor.performTransfer(transfer, originFee, crossChainFee)
            .onSuccess {
                showMessage(resourceManager.getString(R.string.common_transaction_submitted))

                router.finishSendFlow()
            }.onFailure(::showError)

        _transferSubmittingLiveData.value = false
    }

    private suspend fun buildValidationPayload(): AssetTransferPayload {
        val chain = originChain()
        val chainAsset = originAsset()

        return AssetTransferPayload(
            transfer = AssetTransfer(
                sender = currentAccount.first(),
                recipient = transferDraft.recipientAddress,
                originChain = chain,
                destinationChain = destinationChain(),
                originChainAsset = chainAsset,
                amount = transferDraft.amount
            ),
            originFee = transferDraft.originFee,
            commissionAsset = commissionAssetFlow.first(),
            usedAsset = assetFlow.first(),
            crossChainFee = transferDraft.crossChainFee
        )
    }

    private suspend fun createTransferDirectionModel() = if (transferDraft.isCrossChain) {
        TransferDirectionModel(
            originChainUi = mapChainToUi(originChain()),
            originChainLabel = resourceManager.getString(R.string.wallet_send_from_network),
            destinationChainUi = mapChainToUi(destinationChain())
        )
    } else {
        TransferDirectionModel(
            originChainUi = mapChainToUi(originChain()),
            originChainLabel = resourceManager.getString(R.string.common_network),
            destinationChainUi = null
        )
    }
}
