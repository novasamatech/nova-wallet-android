package io.novafoundation.nova.feature_gift_impl.presentation.confirm

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.asLoaded
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_assets.presentation.send.autoFixSendValidationPayload
import io.novafoundation.nova.feature_assets.presentation.send.mapAssetTransferValidationFailureToUI
import io.novafoundation.nova.feature_gift_impl.R
import io.novafoundation.nova.feature_gift_impl.domain.CreateGiftInteractor
import io.novafoundation.nova.feature_gift_impl.domain.GiftId
import io.novafoundation.nova.feature_gift_impl.domain.models.CreateGiftModel
import io.novafoundation.nova.feature_gift_impl.presentation.GiftRouter
import io.novafoundation.nova.feature_gift_impl.presentation.amount.fee.GiftFeeDisplayFormatter
import io.novafoundation.nova.feature_gift_impl.presentation.amount.fee.createForGiftsWithGiftFeeDisplay
import io.novafoundation.nova.feature_gift_impl.presentation.common.buildGiftValidationPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.WeightedAssetTransfer
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountSign
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class CreateGiftConfirmViewModel(
    private val router: GiftRouter,
    private val chainRegistry: ChainRegistry,
    private val validationExecutor: ValidationExecutor,
    private val assetUseCase: ArbitraryAssetUseCase,
    private val walletUiUseCase: WalletUiUseCase,
    private val payload: CreateGiftConfirmPayload,
    private val amountFormatter: AmountFormatter,
    private val resourceManager: ResourceManager,
    private val externalActions: ExternalActions.Presentation,
    private val createGiftInteractor: CreateGiftInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor {

    private val chainFlow = chainRegistry.chainFlow(payload.assetPayload.chainId)
    private val chainAssetFlow = chainFlow.map { it.assetsById.getValue(payload.assetPayload.chainAssetId) }

    private val metaAccountFlow = selectedAccountUseCase.selectedMetaAccountFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val assetFlow = chainAssetFlow.flatMapLatest(assetUseCase::assetFlow)
        .shareInBackground()

    val chainModelFlow = chainFlow.map {
        mapChainToUi(it)
    }.shareInBackground()

    val wallet = walletUiUseCase.selectedWalletUiFlow()
        .inBackground()
        .share()

    val senderGiftAccount = combine(metaAccountFlow, chainFlow) { metaAccount, chain ->
        createAddressModel(
            address = metaAccount.requireAddressIn(chain),
            chain = chain
        )
    }
        .inBackground()
        .share()

    private val feeFormatter = GiftFeeDisplayFormatter(amountFormatter)
    val feeMixin = feeLoaderMixinFactory.createForGiftsWithGiftFeeDisplay(
        chainAssetFlow,
        feeFormatter
    )

    val totalAmountModel = assetFlow.map { asset ->
        amountFormatter.formatAmountToAmountModel(payload.amount, asset, AmountConfig(tokenAmountSign = AmountSign.NEGATIVE))
            .asLoaded()
    }

    val giftAmountModel = assetFlow.map { asset ->
        amountFormatter.formatAmountToAmountModel(payload.amount, asset)
    }

    private val validationInProgressFlow = MutableStateFlow(false)
    val confirmButtonStateLiveData = validationInProgressFlow.map { submitting ->
        if (submitting) {
            DescriptiveButtonState.Loading
        } else {
            DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_confirm))
        }
    }

    init {
        setupFees()
    }

    fun back() {
        router.back()
    }

    fun accountClicked() = launchUnit {
        val chain = chainFlow.first()
        val address = senderGiftAccount.first()
        externalActions.showAddressActions(address.address, chain)
    }

    fun confirmClicked() = launchUnit {
        validationInProgressFlow.value = true
        val fee = feeMixin.awaitFee()
        val chain = chainFlow.first()

        val giftModel = CreateGiftModel(
            senderMetaAccount = selectedAccountUseCase.getSelectedMetaAccount(),
            chain = chain,
            chainAsset = chainAssetFlow.first(),
            amount = payload.amount,
        )

        val payload = buildGiftValidationPayload(
            giftModel,
            asset = assetFlow.first(),
            payload.transferringMaxAmount,
            feeMixin.feePaymentCurrency(),
            fee
        )

        validationExecutor.requireValid(
            validationSystem = createGiftInteractor.validationSystemFor(giftModel.chainAsset, viewModelScope),
            payload = payload,
            progressConsumer = validationInProgressFlow.progressConsumer(),
            autoFixPayload = ::autoFixSendValidationPayload,
            validationFailureTransformerCustom = { status, actions ->
                viewModelScope.mapAssetTransferValidationFailureToUI(
                    resourceManager = resourceManager,
                    status = status,
                    actions = actions,
                    setFee = { feeMixin.setFee(fee.replaceSubmission(it)) }
                )
            },
        ) { validPayload ->
            performTransfer(giftModel, validPayload.transfer, validPayload.originFee.submissionFee)
        }
    }

    private fun performTransfer(
        giftModel: CreateGiftModel,
        transfer: WeightedAssetTransfer,
        fee: SubmissionFee
    ) = launch {
        createGiftInteractor.createAndSaveGift(giftModel, transfer, fee, viewModelScope)
            .onSuccess {
                finishCreateGift(giftId = it)
            }.onFailure(::showError)

        validationInProgressFlow.value = false
    }

    private fun finishCreateGift(giftId: GiftId) {
        router.openGiftSharing(giftId)
    }

    private fun setupFees() {
        feeMixin.loadFee {
            val metaAccount = metaAccountFlow.first()
            val chain = chainFlow.first()

            val createGiftModel = CreateGiftModel(
                senderMetaAccount = metaAccount,
                chain = chain,
                chainAsset = chainAssetFlow.first(),
                amount = payload.amount,
            )

            createGiftInteractor.getFee(
                createGiftModel,
                payload.transferringMaxAmount,
                viewModelScope
            )
        }
    }

    private suspend fun createAddressModel(
        address: String,
        chain: Chain
    ) = addressIconGenerator.createAddressModel(
        chain = chain,
        sizeInDp = AddressIconGenerator.SIZE_MEDIUM,
        address = address,
        background = AddressIconGenerator.BACKGROUND_TRANSPARENT,
        addressDisplayUseCase = null
    )
}
