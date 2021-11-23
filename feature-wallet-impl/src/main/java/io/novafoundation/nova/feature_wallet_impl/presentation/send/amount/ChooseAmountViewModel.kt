package io.novafoundation.nova.feature_wallet_impl.presentation.send.amount

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapAssetToAssetModel
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletInteractor
import io.novafoundation.nova.feature_wallet_api.domain.model.Transfer
import io.novafoundation.nova.feature_wallet_api.domain.model.TransferValidityLevel.Error
import io.novafoundation.nova.feature_wallet_api.domain.model.TransferValidityLevel.Ok
import io.novafoundation.nova.feature_wallet_api.domain.model.TransferValidityLevel.Warning
import io.novafoundation.nova.feature_wallet_api.domain.model.TransferValidityStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.WithAmountChooser
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.WithFeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.requireFee
import io.novafoundation.nova.feature_wallet_impl.presentation.AssetPayload
import io.novafoundation.nova.feature_wallet_impl.presentation.WalletRouter
import io.novafoundation.nova.feature_wallet_impl.presentation.send.TransferDraft
import io.novafoundation.nova.feature_wallet_impl.presentation.send.TransferValidityChecks
import io.novafoundation.nova.feature_wallet_impl.presentation.send.phishing.warning.api.PhishingWarningMixin
import io.novafoundation.nova.feature_wallet_impl.presentation.send.phishing.warning.api.PhishingWarningPresentation
import io.novafoundation.nova.feature_wallet_impl.presentation.send.phishing.warning.api.proceedOrShowPhishingWarning
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlin.time.ExperimentalTime

class ChooseAmountViewModel(
    private val interactor: WalletInteractor,
    private val router: WalletRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val externalActions: ExternalActions.Presentation,
    private val transferValidityChecks: TransferValidityChecks.Presentation,
    private val recipientAddress: String,
    private val assetPayload: AssetPayload,
    private val chainRegistry: ChainRegistry,
    private val feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val resourceManager: ResourceManager,
    private val phishingAddress: PhishingWarningMixin,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
) : BaseViewModel(),
    ExternalActions by externalActions,
    TransferValidityChecks by transferValidityChecks,
    PhishingWarningMixin by phishingAddress,
    WithFeeLoaderMixin,
    WithAmountChooser,
    PhishingWarningPresentation {

    private val chain by lazyAsync { chainRegistry.getChain(assetPayload.chainId) }

    val recipientModelFlow = flowOf { generateAddressModel(recipientAddress) }
        .inBackground()
        .share()

    private val checkingEnoughFundsFlow = MutableStateFlow(false)

    private val assetFlow = interactor.assetFlow(assetPayload.chainId, assetPayload.chainAssetId)
        .inBackground()
        .share()

    override val feeLoaderMixin: FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(assetFlow)

    override val amountChooserMixin: AmountChooserMixin.Presentation = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = assetFlow,
        assetUiMapper = { mapAssetToAssetModel(it, resourceManager) }
    )

    val continueButtonStateLiveData = combine(
        checkingEnoughFundsFlow,
        amountChooserMixin.amountInput
    ) { checkingFunds, amountRaw ->
        when {
            checkingFunds -> ButtonState.PROGRESS
            amountRaw.isNotEmpty() -> ButtonState.NORMAL
            else -> ButtonState.DISABLED
        }
    }

    init {
        listenFee()
    }

    fun nextClicked() = feeLoaderMixin.requireFee(this) {
        checkEnoughFunds(it)
    }

    fun backClicked() {
        router.back()
    }

    fun recipientAddressClicked() = launch {
        val recipientAddress = recipientModelFlow.first().address

        externalActions.showExternalActions(ExternalActions.Type.Address(recipientAddress), chain())
    }


    fun warningConfirmed() {
        openConfirmationScreen()
    }

    override fun proceedAddress(address: String) = feeLoaderMixin.requireFee(this) { fee ->
        launch {
            val transferDraft = buildTransferDraft(fee)

            router.openConfirmTransfer(transferDraft)
        }
    }

    override fun declinePhishingAddress() {
        router.back()
    }

    @OptIn(ExperimentalTime::class)
    private fun listenFee() {
        amountChooserMixin.backPressuredAmount
            .mapLatest(::loadFee)
            .launchIn(viewModelScope)
    }

    private suspend fun loadFee(amount: BigDecimal) {
        feeLoaderMixin.loadFeeSuspending(
            retryScope = viewModelScope,
            feeConstructor = { token ->
                val transfer = Transfer(recipientAddress, amount, token.configuration)

                interactor.getTransferFee(transfer)
            },
            onRetryCancelled = ::backClicked
        )
    }


    private suspend fun generateAddressModel(address: String): AddressModel {
        return addressIconGenerator.createAddressModel(chain(), address, AddressIconGenerator.SIZE_MEDIUM, addressDisplayUseCase)
    }

    private fun checkEnoughFunds(fee: BigDecimal) {
        checkingEnoughFundsFlow.value = true

        viewModelScope.launch {
            val amount = amountChooserMixin.amount.first()
            val asset = interactor.getCurrentAsset(assetPayload.chainId, assetPayload.chainAssetId)
            val transfer = Transfer(recipientAddress, amount, asset.token.configuration)

            interactor.checkTransferValidityStatus(transfer, fee)
                .onSuccess(::processHasEnoughFunds)
                .onFailure(::showError)

            checkingEnoughFundsFlow.value = false
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

    private suspend fun buildTransferDraft(
        fee: BigDecimal,
    ): TransferDraft {
        val amount = amountChooserMixin.amount.first()

        return TransferDraft(amount, fee, assetPayload, recipientAddress)
    }
}
