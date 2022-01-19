package io.novafoundation.nova.feature_wallet_impl.presentation.send.confirm

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
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapAssetToAssetModel
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletInteractor
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.WithAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.WithFeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_impl.R
import io.novafoundation.nova.feature_wallet_impl.domain.send.SendInteractor
import io.novafoundation.nova.feature_wallet_impl.presentation.WalletRouter
import io.novafoundation.nova.feature_wallet_impl.presentation.send.TransferDraft
import io.novafoundation.nova.feature_wallet_impl.presentation.send.mapAssetTransferValidationFailureToUI
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ConfirmTransferViewModel(
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
    feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
    val transferDraft: TransferDraft,
) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor,
    WithFeeLoaderMixin,
    WithAmountChooser {

    private val chain by lazyAsync { chainRegistry.getChain(transferDraft.assetPayload.chainId) }

    private val chainAsset by lazyAsync { chainRegistry.asset(transferDraft.assetPayload.chainId, transferDraft.assetPayload.chainAssetId) }

    private val assetFlow = interactor.assetFlow(transferDraft.assetPayload.chainId, transferDraft.assetPayload.chainAssetId)
        .inBackground()
        .share()

    private val commissionAssetFlow = interactor.utilityAssetFlow(transferDraft.assetPayload.chainId)
        .inBackground()
        .share()

    override val feeLoaderMixin: FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(commissionAssetFlow)

    override val amountChooserMixin: AmountChooserMixin.Presentation = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = assetFlow,
        assetUiMapper = { mapAssetToAssetModel(it, resourceManager) }
    )

    private val currentAccount = selectedAccountUseCase.selectedMetaAccountFlow()
        .inBackground()
        .share()

    val recipientModel = flowOf {
        addressIconGenerator.createAddressModel(
            chain = chain(),
            sizeInDp = AddressIconGenerator.SIZE_MEDIUM,
            address = transferDraft.recipientAddress,
            addressDisplayUseCase = addressDisplayUseCase
        )
    }
        .inBackground()
        .share()

    val senderModel = currentAccount.mapLatest {
        addressIconGenerator.createAddressModel(
            chain = chain(),
            sizeInDp = AddressIconGenerator.SIZE_MEDIUM,
            address = it.addressIn(chain())!!,
            addressDisplayUseCase = addressDisplayUseCase
        )
    }
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
        externalActions.showExternalActions(ExternalActions.Type.Address(address), chain())
    }

    fun submitClicked() = launch {
        val payload = buildValidationPayload()

        validationExecutor.requireValid(
            validationSystem = sendInteractor.validationSystemFor(payload.transfer.chainAsset),
            payload = payload,
            progressConsumer = _transferSubmittingLiveData.progressConsumer(),
            validationFailureTransformer = { mapAssetTransferValidationFailureToUI(resourceManager, it) }
        ) { validPayload ->
            performTransfer(validPayload.transfer, validPayload.fee)
        }
    }

    private fun setInitialState() = launch {
        feeLoaderMixin.setFee(transferDraft.fee)

        amountChooserMixin.setAmount(transferDraft.amount)
    }

    private fun performTransfer(transfer: AssetTransfer, fee: BigDecimal) = launch {
        sendInteractor.performTransfer(transfer, fee)
            .onSuccess {
                showMessage(resourceManager.getString(R.string.common_transaction_submitted))

                router.finishSendFlow()
            }.onFailure(::showError)

        _transferSubmittingLiveData.value = false
    }

    private suspend fun buildValidationPayload(): AssetTransferPayload {
        val chain = chain()
        val chainAsset = chainAsset()

        return AssetTransferPayload(
            transfer = AssetTransfer(
                sender = currentAccount.first(),
                recipient = chain.accountIdOf(transferDraft.recipientAddress),
                chain = chain,
                chainAsset = chainAsset,
                amount = transferDraft.amount
            ),
            fee = transferDraft.fee,
            commissionAsset = commissionAssetFlow.first(),
            usedAsset = assetFlow.first()
        )
    }
}
