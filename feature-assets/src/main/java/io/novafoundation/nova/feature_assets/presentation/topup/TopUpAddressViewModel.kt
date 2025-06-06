package io.novafoundation.nova.feature_assets.presentation.topup

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.setAddress
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.send.autoFixSendValidationPayload
import io.novafoundation.nova.feature_assets.presentation.send.common.buildAssetTransfer
import io.novafoundation.nova.feature_assets.presentation.send.mapAssetTransferValidationFailureToUI
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.WeightedAssetTransfer
import io.novafoundation.nova.feature_wallet_api.domain.model.OriginFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.isMaxAction
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setBlockedAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefault
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TopUpAddressViewModel(
    private val chainRegistry: ChainRegistry,
    private val interactor: WalletInteractor,
    private val sendInteractor: SendInteractor,
    private val router: AssetsRouter,
    private val payload: TopUpAddressPayload,
    private val validationExecutor: ValidationExecutor,
    private val resourceManager: ResourceManager,
    private val responder: TopUpAddressResponder,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    selectedAccountUseCase: SelectedAccountUseCase,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
    addressInputMixinFactory: AddressInputMixinFactory,
) : BaseViewModel(), Validatable by validationExecutor {

    private val chainWithAssetFlow = flowOf { chainRegistry.chainWithAsset(payload.asset.chainId, payload.asset.chainAssetId) }

    private val chainFlow = chainWithAssetFlow.map { it.chain }
    private val chainAssetFlow = chainWithAssetFlow.map { it.asset }

    private val selectedAccount = selectedAccountUseCase.selectedMetaAccountFlow()
        .inBackground()
        .share()

    private val sendInProgressFlow = MutableStateFlow(false)

    private val assetFlow = chainAssetFlow.flatMapLatest(interactor::assetFlow)
        .shareInBackground()

    val addressInputMixin = with(addressInputMixinFactory) {
        create(
            inputSpecProvider = singleChainInputSpec(chainFlow),
            errorDisplayer = this@TopUpAddressViewModel::showError,
            showAccountEvent = null,
            coroutineScope = this@TopUpAddressViewModel,
        )
    }

    val feeMixin = feeLoaderMixinFactory.createDefault(this, chainAssetFlow)

    val amountChooserMixin: AmountChooserMixin.Presentation = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = assetFlow,
        maxActionProvider = null
    )

    val titleFlow = flowOf { payload.screenTitle }
        .shareInBackground()

    val continueButtonState = sendInProgressFlow.map { isSending ->
        when {
            isSending -> ButtonState.PROGRESS
            else -> ButtonState.NORMAL
        }
    }

    init {
        addressInputMixin.setAddress(payload.address)
        amountChooserMixin.setBlockedAmount(payload.amount)

        setupFees()
    }

    fun nextClicked() = launch {
        sendInProgressFlow.value = true

        val fee = feeMixin.awaitFee()
        val originFee = OriginFee(fee, null)

        val amountState = amountChooserMixin.amountState.first()
        val transfer = buildTransfer(feeMixin.feePaymentCurrency(), amountState.inputKind.isMaxAction())

        val payload = AssetTransferPayload(
            transfer = WeightedAssetTransfer(
                assetTransfer = transfer,
                fee = originFee,
            ),
            crossChainFee = null,
            originFee = originFee,
            originCommissionAsset = feeMixin.feeAsset(),
            originUsedAsset = assetFlow.first()
        )

        validationExecutor.requireValid(
            validationSystem = sendInteractor.validationSystemFor(payload.transfer, viewModelScope),
            payload = payload,
            progressConsumer = sendInProgressFlow.progressConsumer(),
            autoFixPayload = ::autoFixSendValidationPayload,
            validationFailureTransformerCustom = { status, actions ->
                viewModelScope.mapAssetTransferValidationFailureToUI(
                    resourceManager = resourceManager,
                    status = status,
                    actions = actions,
                    setFee = feeMixin
                )
            },
        ) {
            sendInProgressFlow.value = false
            transferTokensAndFinishFlow(it)
        }
    }

    fun backClicked() {
        responder.respond(TopUpAddressResponder.Response.Cancel)
        router.finishTopUp()
    }

    private fun transferTokensAndFinishFlow(payload: AssetTransferPayload) = launch {
        sendInteractor.performTransfer(payload.transfer, payload.originFee, null, viewModelScope)

        responder.respond(TopUpAddressResponder.Response.Success)
        router.finishTopUp()
    }

    private fun setupFees() {
        feeMixin.connectWith(
            feeMixin.feeChainAssetFlow,
            amountChooserMixin.amountState
        ) { feePaymentCurrency, commissionAsset, amountState ->
            val assetTransfer = buildTransfer(feePaymentCurrency, amountState.inputKind.isMaxAction())

            sendInteractor.getSubmissionFee(assetTransfer, viewModelScope)
        }
    }

    private suspend fun buildTransfer(
        feePaymentCurrency: FeePaymentCurrency,
        transferringMaxAmount: Boolean
    ): AssetTransfer {
        val chainWithAsset = chainWithAssetFlow.first()
        val amount = amountChooserMixin.amount.first()
        val address = addressInputMixin.getAddress()
        return buildAssetTransfer(
            metaAccount = selectedAccount.first(),
            feePaymentCurrency = feePaymentCurrency,
            origin = chainWithAsset,
            destination = chainWithAsset,
            amount = amount,
            address = address,
            transferringMaxAmount = transferringMaxAmount
        )
    }
}
