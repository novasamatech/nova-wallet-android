package io.novafoundation.nova.feature_wallet_impl.presentation.send.confirm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.map
import io.novafoundation.nova.common.utils.requireException
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.NotValidTransferStatus
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletInteractor
import io.novafoundation.nova.feature_wallet_api.domain.model.Transfer
import io.novafoundation.nova.feature_wallet_api.domain.model.TransferValidityLevel
import io.novafoundation.nova.feature_wallet_api.domain.model.TransferValidityStatus
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapAssetToAssetModel
import io.novafoundation.nova.feature_wallet_impl.presentation.WalletRouter
import io.novafoundation.nova.feature_wallet_impl.presentation.send.BalanceDetailsBottomSheet
import io.novafoundation.nova.feature_wallet_impl.presentation.send.TransferDraft
import io.novafoundation.nova.feature_wallet_impl.presentation.send.TransferValidityChecks
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val ICON_IN_DP = 24

class ConfirmTransferViewModel(
    private val interactor: WalletInteractor,
    private val router: WalletRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val externalActions: ExternalActions.Presentation,
    private val walletConstants: WalletConstants,
    private val transferValidityChecks: TransferValidityChecks.Presentation,
    private val chainRegistry: ChainRegistry,
    val transferDraft: TransferDraft,
) : BaseViewModel(),
    ExternalActions by externalActions,
    TransferValidityChecks by transferValidityChecks {

    private val chain by lazyAsync { chainRegistry.getChain(transferDraft.assetPayload.chainId) }

    private val _showBalanceDetailsEvent = MutableLiveData<Event<BalanceDetailsBottomSheet.Payload>>()
    val showBalanceDetailsEvent: LiveData<Event<BalanceDetailsBottomSheet.Payload>> = _showBalanceDetailsEvent

    val recipientModel = liveData { emit(getAddressIcon()) }

    private val _transferSubmittingLiveData = MutableLiveData(false)

    val sendButtonStateLiveData = _transferSubmittingLiveData.map { submitting ->
        if (submitting) {
            ButtonState.PROGRESS
        } else {
            ButtonState.NORMAL
        }
    }

    val assetLiveData = interactor.assetFlow(transferDraft.assetPayload.chainId, transferDraft.assetPayload.chainAssetId)
        .map(::mapAssetToAssetModel)
        .inBackground()
        .asLiveData()

    fun backClicked() {
        router.back()
    }

    fun copyRecipientAddressClicked() = launch {
        externalActions.showExternalActions(ExternalActions.Type.Address(transferDraft.recipientAddress), chain())
    }

    fun availableBalanceClicked() {
        val assetModel = assetLiveData.value ?: return

        launch {
            val amountInPlanks = walletConstants.existentialDeposit(assetModel.token.configuration.chainId)
            val existentialDeposit = assetModel.token.configuration.amountFromPlanks(amountInPlanks)

            _showBalanceDetailsEvent.value = Event(BalanceDetailsBottomSheet.Payload(assetModel, transferDraft, existentialDeposit))
        }
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

    private fun performTransfer(suppressWarnings: Boolean) {
        val chainAsset = assetLiveData.value?.token?.configuration ?: return
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

    private suspend fun getAddressIcon(): AddressModel {
        return addressIconGenerator.createAddressModel(chain(), transferDraft.recipientAddress, ICON_IN_DP)
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
