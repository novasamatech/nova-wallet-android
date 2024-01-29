package io.novafoundation.nova.feature_assets.presentation.send.confirm

import androidx.lifecycle.viewModelScope
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
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.send.TransferDraft
import io.novafoundation.nova.feature_assets.presentation.send.autoFixSendValidationPayload
import io.novafoundation.nova.feature_assets.presentation.send.common.buildAssetTransfer
import io.novafoundation.nova.feature_assets.presentation.send.confirm.hints.ConfirmSendHintsMixinFactory
import io.novafoundation.nova.feature_assets.presentation.send.isCrossChain
import io.novafoundation.nova.feature_assets.presentation.send.mapAssetTransferValidationFailureToUI
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.WeightedAssetTransfer
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.SimpleFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitDecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitOptionalDecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountSign
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
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
    feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    val transferDraft: TransferDraft,
) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor {

    private val originChain by lazyAsync { chainRegistry.getChain(transferDraft.origin.chainId) }
    private val originAsset by lazyAsync { chainRegistry.asset(transferDraft.origin.chainId, transferDraft.origin.chainAssetId) }

    private val destinationChain by lazyAsync { chainRegistry.getChain(transferDraft.destination.chainId) }
    private val destinationChainAsset by lazyAsync { chainRegistry.asset(transferDraft.destination.chainId, transferDraft.destination.chainAssetId) }

    private val assetFlow = interactor.assetFlow(transferDraft.origin.chainId, transferDraft.origin.chainAssetId)
        .inBackground()
        .share()

    private val commissionAssetFlow = interactor.commissionAssetFlow(transferDraft.origin.chainId)
        .inBackground()
        .share()

    private val currentAccount = selectedAccountUseCase.selectedMetaAccountFlow()
        .inBackground()
        .share()

    val originFeeMixin: FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(commissionAssetFlow)
    val crossChainFeeMixin: FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(commissionAssetFlow)

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
        externalActions.showExternalActions(ExternalActions.Type.Address(address), chain())
    }

    fun submitClicked() = launch {
        val payload = buildValidationPayload()

        validationExecutor.requireValid(
            validationSystem = sendInteractor.validationSystemFor(payload.transfer),
            payload = payload,
            progressConsumer = _transferSubmittingLiveData.progressConsumer(),
            autoFixPayload = ::autoFixSendValidationPayload,
            validationFailureTransformerCustom = { status, actions ->
                viewModelScope.mapAssetTransferValidationFailureToUI(
                    resourceManager = resourceManager,
                    status = status,
                    actions = actions,
                    feeLoaderMixin = originFeeMixin
                )
            },
        ) { validPayload ->
            performTransfer(validPayload.transfer, validPayload.originFee, validPayload.crossChainFee)
        }
    }

    private fun setupFee() = launch {
        launch {
            val assetTransfer = buildTransfer()
            val planks = originAsset().planksFromAmount(assetTransfer.amount)

            originFeeMixin.invalidateFee()
            crossChainFeeMixin.invalidateFee()

            val transferFeeModel = sendInteractor.getFee(planks, assetTransfer)
            val originFee = SimpleFee(transferFeeModel.originFee)
            val crossChainFee = transferFeeModel.crossChainFee?.let { SimpleFee(it) }

            originFeeMixin.setFee(originFee)
            crossChainFeeMixin.setFee(crossChainFee)
        }
    }

    private suspend fun buildTransfer(): AssetTransfer {
        val originChainWithAsset = ChainWithAsset(originChain(), originAsset())
        val destinationChainWithAsset = ChainWithAsset(destinationChain(), destinationChainAsset())
        val amount = transferDraft.amount
        val address = transferDraft.recipientAddress

        return buildAssetTransfer(
            metaAccount = selectedAccountUseCase.getSelectedMetaAccount(),
            commissionAsset = commissionAssetFlow.first(),
            origin = originChainWithAsset,
            destination = destinationChainWithAsset,
            amount = amount,
            address = address
        )
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
        originFee: DecimalFee,
        crossChainFee: DecimalFee?
    ) = launch {
        sendInteractor.performTransfer(transfer, originFee, crossChainFee?.genericFee?.networkFee)
            .onSuccess {
                showMessage(resourceManager.getString(R.string.common_transaction_submitted))

                finishSendFlow()
            }.onFailure(::showError)

        _transferSubmittingLiveData.value = false
    }

    private suspend fun finishSendFlow() {
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

        val originFee = originFeeMixin.awaitDecimalFee()

        return AssetTransferPayload(
            transfer = WeightedAssetTransfer(
                sender = currentAccount.first(),
                recipient = transferDraft.recipientAddress,
                originChain = chain,
                destinationChain = destinationChain(),
                destinationChainAsset = destinationChainAsset(),
                originChainAsset = chainAsset,
                amount = transferDraft.amount,
                commissionAssetToken = commissionAssetFlow.first().token,
                decimalFee = originFee,
            ),
            originFee = originFee,
            originCommissionAsset = commissionAssetFlow.first(),
            originUsedAsset = assetFlow.first(),
            crossChainFee = crossChainFeeMixin.awaitOptionalDecimalFee()
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
