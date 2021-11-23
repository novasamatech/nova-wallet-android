package io.novafoundation.nova.feature_wallet_impl.presentation.send.confirm

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.requireException
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapAssetToAssetModel
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.NotValidTransferStatus
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletInteractor
import io.novafoundation.nova.feature_wallet_api.domain.model.Transfer
import io.novafoundation.nova.feature_wallet_api.domain.model.TransferValidityLevel
import io.novafoundation.nova.feature_wallet_api.domain.model.TransferValidityStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.WithAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.WithFeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_impl.presentation.WalletRouter
import io.novafoundation.nova.feature_wallet_impl.presentation.send.TransferDraft
import io.novafoundation.nova.feature_wallet_impl.presentation.send.TransferValidityChecks
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

class ConfirmTransferViewModel(
    private val interactor: WalletInteractor,
    private val router: WalletRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val externalActions: ExternalActions.Presentation,
    private val transferValidityChecks: TransferValidityChecks.Presentation,
    private val chainRegistry: ChainRegistry,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val resourceManager: ResourceManager,
    feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
    val transferDraft: TransferDraft,
) : BaseViewModel(),
    ExternalActions by externalActions,
    WithFeeLoaderMixin,
    WithAmountChooser,
    TransferValidityChecks by transferValidityChecks {

    private val chain by lazyAsync { chainRegistry.getChain(transferDraft.assetPayload.chainId) }

    private val assetFlow = interactor.assetFlow(transferDraft.assetPayload.chainId, transferDraft.assetPayload.chainAssetId)
        .inBackground()
        .share()

    override val feeLoaderMixin: FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(assetFlow)
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

    fun submitClicked() {
        performTransfer(suppressWarnings = false)
    }

    fun warningConfirmed() {
        performTransfer(suppressWarnings = true)
    }

    fun errorAcknowledged() {
        router.back()
    }

    private fun setInitialState() = launch {
        feeLoaderMixin.setFee(transferDraft.fee)

        amountChooserMixin.setAmount(transferDraft.amount)
    }

    private fun performTransfer(suppressWarnings: Boolean) = launch {
        val chainAsset = assetFlow.first().token.configuration
        val maxAllowedStatusLevel = if (suppressWarnings) TransferValidityLevel.Warning else TransferValidityLevel.Ok

        _transferSubmittingLiveData.value = true

        viewModelScope.launch {
            val result = interactor.performTransfer(createTransfer(chainAsset), transferDraft.fee, maxAllowedStatusLevel)

            if (result.isSuccess) {
                router.finishSendFlow()
            } else {
                val error = result.requireException()

                if (error is NotValidTransferStatus) {
                    processInvalidStatus(error.status)
                } else {
                    showError(error)
                }
            }

            _transferSubmittingLiveData.value = false
        }
    }

    private fun processInvalidStatus(status: TransferValidityStatus) {
        when (status) {
            is TransferValidityLevel.Warning.Status -> transferValidityChecks.showTransferWarning(status)
            is TransferValidityLevel.Error.Status -> transferValidityChecks.showTransferError(status)
        }
    }

    private fun createTransfer(token: Chain.Asset): Transfer {
        return with(transferDraft) {
            Transfer(
                recipient = recipientAddress,
                amount = amount,
                chainAsset = token
            )
        }
    }
}
