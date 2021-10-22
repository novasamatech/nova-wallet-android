package io.novafoundation.nova.feature_wallet_impl.presentation.send.amount

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.combine
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.map
import io.novafoundation.nova.common.utils.requireValue
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletInteractor
import io.novafoundation.nova.feature_wallet_api.domain.model.Fee
import io.novafoundation.nova.feature_wallet_api.domain.model.Transfer
import io.novafoundation.nova.feature_wallet_api.domain.model.TransferValidityLevel.Error
import io.novafoundation.nova.feature_wallet_api.domain.model.TransferValidityLevel.Ok
import io.novafoundation.nova.feature_wallet_api.domain.model.TransferValidityLevel.Warning
import io.novafoundation.nova.feature_wallet_api.domain.model.TransferValidityStatus
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_impl.R
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapAssetToAssetModel
import io.novafoundation.nova.feature_wallet_impl.presentation.AssetPayload
import io.novafoundation.nova.feature_wallet_impl.presentation.WalletRouter
import io.novafoundation.nova.feature_wallet_impl.presentation.send.BalanceDetailsBottomSheet
import io.novafoundation.nova.feature_wallet_impl.presentation.send.TransferDraft
import io.novafoundation.nova.feature_wallet_impl.presentation.send.TransferValidityChecks
import io.novafoundation.nova.feature_wallet_impl.presentation.send.phishing.warning.api.PhishingWarningMixin
import io.novafoundation.nova.feature_wallet_impl.presentation.send.phishing.warning.api.PhishingWarningPresentation
import io.novafoundation.nova.feature_wallet_impl.presentation.send.phishing.warning.api.proceedOrShowPhishingWarning
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

private const val AVATAR_SIZE_DP = 24

enum class RetryReason(val reasonRes: Int) {
    CHECK_ENOUGH_FUNDS(R.string.choose_amount_error_balance),
    LOAD_FEE(R.string.choose_amount_error_fee)
}

class ChooseAmountViewModel(
    private val interactor: WalletInteractor,
    private val router: WalletRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val externalActions: ExternalActions.Presentation,
    private val transferValidityChecks: TransferValidityChecks.Presentation,
    private val walletConstants: WalletConstants,
    private val recipientAddress: String,
    private val assetPayload: AssetPayload,
    private val chainRegistry: ChainRegistry,
    private val phishingAddress: PhishingWarningMixin
) : BaseViewModel(),
    ExternalActions by externalActions,
    TransferValidityChecks by transferValidityChecks,
    PhishingWarningMixin by phishingAddress,
    PhishingWarningPresentation {

    private val chain by lazyAsync { chainRegistry.getChain(assetPayload.chainId) }

    val recipientModelLiveData = liveData {
        emit(generateAddressModel(recipientAddress))
    }

    private val amountEvents = MutableStateFlow("0")
    private val amountRawLiveData = amountEvents.asLiveData()

    private val _feeLoadingLiveData = MutableLiveData(true)
    val feeLoadingLiveData = _feeLoadingLiveData

    val feeLiveData = feeFlow().asLiveData()

    private val _feeErrorLiveData = MutableLiveData<Event<RetryReason>>()
    val feeErrorLiveData = _feeErrorLiveData

    private val checkingEnoughFundsLiveData = MutableLiveData(false)

    private val _showBalanceDetailsEvent = MutableLiveData<Event<BalanceDetailsBottomSheet.Payload>>()
    val showBalanceDetailsEvent: LiveData<Event<BalanceDetailsBottomSheet.Payload>> = _showBalanceDetailsEvent

    val assetLiveData = liveData {
        val asset = interactor.getCurrentAsset(assetPayload.chainId, assetPayload.chainAssetId)

        emit(mapAssetToAssetModel(asset))
    }

    private val minimumPossibleAmountLiveData = assetLiveData.map {
        it.token.configuration.amountFromPlanks(BigInteger.ONE)
    }

    val continueButtonStateLiveData = combine(
        feeLoadingLiveData,
        feeLiveData,
        checkingEnoughFundsLiveData,
        amountRawLiveData,
        minimumPossibleAmountLiveData
    ) { (feeLoading: Boolean, fee: Fee?, checkingFunds: Boolean, amountRaw: String, minimumPossibleAmount: BigDecimal) ->
        when {
            feeLoading || checkingFunds -> ButtonState.PROGRESS
            fee != null && fee.transferAmount >= minimumPossibleAmount
                && amountRaw.isNotEmpty() -> ButtonState.NORMAL
            else -> ButtonState.DISABLED
        }
    }

    fun nextClicked() {
        checkEnoughFunds()
    }

    fun amountChanged(newAmountRaw: String) {
        viewModelScope.launch {
            amountEvents.emit(newAmountRaw)
        }
    }

    fun backClicked() {
        router.back()
    }

    fun retry(retryReason: RetryReason) {
        when (retryReason) {
            RetryReason.LOAD_FEE -> retryLoadFee()
            RetryReason.CHECK_ENOUGH_FUNDS -> checkEnoughFunds()
        }
    }

    fun recipientAddressClicked() = launch {
        val recipientAddress = recipientModelLiveData.value?.address ?: return@launch

        externalActions.showExternalActions(ExternalActions.Type.Address(recipientAddress), chain())
    }

    fun availableBalanceClicked() {
        val transferDraft = buildTransferDraft() ?: return
        val assetModel = assetLiveData.value ?: return

        launch {
            val amountInPlanks = walletConstants.existentialDeposit(assetModel.token.configuration.chainId)
            val existentialDeposit = assetModel.token.configuration.amountFromPlanks(amountInPlanks)

            _showBalanceDetailsEvent.value = Event(BalanceDetailsBottomSheet.Payload(assetModel, transferDraft, existentialDeposit))
        }
    }

    fun warningConfirmed() {
        openConfirmationScreen()
    }

    override fun proceedAddress(address: String) {
        val transferDraft = buildTransferDraft() ?: return

        router.openConfirmTransfer(transferDraft)
    }

    override fun declinePhishingAddress() {
        router.back()
    }

    @OptIn(ExperimentalTime::class)
    private fun feeFlow(): Flow<Fee?> = amountEvents
        .mapNotNull(String::toBigDecimalOrNull)
        .debounce(500.milliseconds)
        .distinctUntilChanged()
        .onEach { _feeLoadingLiveData.postValue(true) }
        .mapLatest<BigDecimal, Fee?> { amount ->
            val asset = interactor.getCurrentAsset(assetPayload.chainId, assetPayload.chainAssetId)
            val transfer = Transfer(recipientAddress, amount, asset.token.configuration)

            interactor.getTransferFee(transfer)
        }
        .catch {
            it.printStackTrace()

            _feeErrorLiveData.postValue(Event(RetryReason.LOAD_FEE))

            emit(null)
        }.onEach {
            _feeLoadingLiveData.value = false
        }

    private suspend fun generateAddressModel(address: String): AddressModel {
        return addressIconGenerator.createAddressModel(chain(), address, AVATAR_SIZE_DP)
    }

    private fun checkEnoughFunds() {
        val fee = feeLiveData.value ?: return

        checkingEnoughFundsLiveData.value = true

        viewModelScope.launch {
            val asset = interactor.getCurrentAsset(assetPayload.chainId, assetPayload.chainAssetId)
            val transfer = Transfer(recipientAddress, fee.transferAmount, asset.token.configuration)

            val result = interactor.checkTransferValidityStatus(transfer)

            if (result.isSuccess) {
                processHasEnoughFunds(result.requireValue())
            } else {
                _feeErrorLiveData.value = Event(RetryReason.CHECK_ENOUGH_FUNDS)
            }

            checkingEnoughFundsLiveData.value = false
        }
    }

    private fun processHasEnoughFunds(status: TransferValidityStatus) {
        when (status) {
            is Ok -> openConfirmationScreen()
            is Warning.Status -> transferValidityChecks.showTransferWarning(status)
            is Error.Status -> transferValidityChecks.showTransferError(status)
        }
    }

    private fun openConfirmationScreen() {
        viewModelScope.launch {
            proceedOrShowPhishingWarning(recipientAddress)
        }
    }

    private fun buildTransferDraft(): TransferDraft? {
        val fee = feeLiveData.value ?: return null

        return TransferDraft(fee.transferAmount, fee.feeAmount, assetPayload, recipientAddress)
    }

    private fun retryLoadFee() {
        amountChanged(amountEvents.value)
    }
}
