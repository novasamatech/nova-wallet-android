package jp.co.soramitsu.feature_wallet_impl.presentation.send.confirm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import jp.co.soramitsu.common.address.AddressIconGenerator
import jp.co.soramitsu.common.address.AddressModel
import jp.co.soramitsu.common.base.BaseViewModel
import jp.co.soramitsu.common.utils.Event
import jp.co.soramitsu.common.utils.inBackground
import jp.co.soramitsu.common.utils.invoke
import jp.co.soramitsu.common.utils.lazyAsync
import jp.co.soramitsu.common.utils.map
import jp.co.soramitsu.common.utils.requireException
import jp.co.soramitsu.common.view.ButtonState
import jp.co.soramitsu.feature_account_api.presenatation.account.icon.createAddressModel
import jp.co.soramitsu.feature_account_api.presenatation.actions.ExternalActions
import jp.co.soramitsu.feature_wallet_api.domain.interfaces.NotValidTransferStatus
import jp.co.soramitsu.feature_wallet_api.domain.interfaces.WalletConstants
import jp.co.soramitsu.feature_wallet_api.domain.interfaces.WalletInteractor
import jp.co.soramitsu.feature_wallet_api.domain.model.Transfer
import jp.co.soramitsu.feature_wallet_api.domain.model.TransferValidityLevel
import jp.co.soramitsu.feature_wallet_api.domain.model.TransferValidityStatus
import jp.co.soramitsu.feature_wallet_api.domain.model.amountFromPlanks
import jp.co.soramitsu.feature_wallet_impl.data.mappers.mapAssetToAssetModel
import jp.co.soramitsu.feature_wallet_impl.presentation.WalletRouter
import jp.co.soramitsu.feature_wallet_impl.presentation.send.BalanceDetailsBottomSheet
import jp.co.soramitsu.feature_wallet_impl.presentation.send.TransferDraft
import jp.co.soramitsu.feature_wallet_impl.presentation.send.TransferValidityChecks
import jp.co.soramitsu.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.runtime.multiNetwork.chain.model.Chain
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
