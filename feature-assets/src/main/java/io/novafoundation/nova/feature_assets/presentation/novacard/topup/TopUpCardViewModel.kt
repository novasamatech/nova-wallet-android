package io.novafoundation.nova.feature_assets.presentation.novacard.topup

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.setAddress
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.send.SendInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.send.autoFixSendValidationPayload
import io.novafoundation.nova.feature_assets.presentation.send.common.buildAssetTransfer
import io.novafoundation.nova.feature_assets.presentation.send.mapAssetTransferValidationFailureToUI
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfer
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.WeightedAssetTransfer
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.OriginGenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.SimpleGenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitDecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.commissionAsset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.connectGenericWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.createGenericChangeableFee
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TopUpCardViewModel(
    private val chainRegistry: ChainRegistry,
    private val interactor: WalletInteractor,
    private val sendInteractor: SendInteractor,
    private val router: AssetsRouter,
    private val payload: TopUpCardPayload,
    private val validationExecutor: ValidationExecutor,
    private val resourceManager: ResourceManager,
    feeLoaderMixinFactory: FeeLoaderMixin.Factory,
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
            errorDisplayer = this@TopUpCardViewModel::showError,
            showAccountEvent = null,
            coroutineScope = this@TopUpCardViewModel,
        )
    }

    val amountChooserMixin: AmountChooserMixin.Presentation = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = assetFlow,
        balanceLabel = R.string.wallet_balance_transferable,
        balanceField = Asset::transferable,
    )

    val feeMixin = feeLoaderMixinFactory.createGenericChangeableFee<OriginGenericFee>(assetFlow, coroutineScope)

    val titleFlow = chainAssetFlow.map {
        resourceManager.getString(R.string.fragment_top_up_card_title, it.symbol)
    }

    val continueButtonState = sendInProgressFlow.map { isSending ->
        when {
            isSending -> ButtonState.PROGRESS
            else -> ButtonState.NORMAL
        }
    }

    init {
        addressInputMixin.setAddress(payload.address)
        amountChooserMixin.setAmount(payload.amount)

        setupFees()
    }

    fun nextClicked() = launch {
        sendInProgressFlow.value = true

        val fee = feeMixin.awaitDecimalFee()

        val transfer = buildTransfer(feeMixin.commissionAsset())

        val payload = AssetTransferPayload(
            transfer = WeightedAssetTransfer(
                assetTransfer = transfer,
                fee = fee,
            ),
            crossChainFee = null,
            originFee = fee,
            originCommissionAsset = feeMixin.commissionAsset(),
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
                    feeLoaderMixin = feeMixin
                )
            },
        ) {
            sendInProgressFlow.value = false
            transferTokensAndFinishFlow(it)

            router.back()
        }
    }

    fun backClicked() {
        router.back()
    }

    private fun transferTokensAndFinishFlow(payload: AssetTransferPayload) = launch {
        sendInteractor.performTransfer(payload.transfer, payload.originFee, null, viewModelScope)

        router.back()
    }

    private fun setupFees() {
        feeMixin.connectGenericWith(
            feeMixin.commissionAssetFlow(),
            scope = viewModelScope,
            feeConstructor = { commissionAsset ->
                val assetTransfer = buildTransfer(commissionAsset)

                val originFee = sendInteractor.getOriginFee(assetTransfer, viewModelScope)
                SimpleGenericFee(originFee)
            }
        )
    }

    private suspend fun buildTransfer(
        commissionAsset: Asset
    ): AssetTransfer {
        val chainWithAsset = chainWithAssetFlow.first()
        val amount = amountChooserMixin.amount.first()
        val address = addressInputMixin.getAddress()
        return buildAssetTransfer(
            metaAccount = selectedAccount.first(),
            commissionAsset = commissionAsset,
            origin = chainWithAsset,
            destination = chainWithAsset,
            amount = amount,
            address = address
        )
    }
}
