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
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_assets.presentation.send.autoFixSendValidationPayload
import io.novafoundation.nova.feature_assets.presentation.send.mapAssetTransferValidationFailureToUI
import io.novafoundation.nova.feature_gift_impl.R
import io.novafoundation.nova.feature_gift_impl.domain.CreateGiftInteractor
import io.novafoundation.nova.feature_gift_impl.domain.models.CreateGiftModel
import io.novafoundation.nova.feature_gift_impl.presentation.GiftRouter
import io.novafoundation.nova.feature_gift_impl.presentation.amount.fee.GiftFeeDisplayFormatter
import io.novafoundation.nova.feature_gift_impl.presentation.amount.fee.createForGifts
import io.novafoundation.nova.feature_gift_impl.presentation.common.buildGiftValidationPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.WeightedAssetTransfer
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.SendUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.OriginFee
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountSign
import io.novafoundation.nova.runtime.ext.addressOf
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
    private val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
    private val sendUseCase: SendUseCase,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor,
    ExtrinsicNavigationWrapper by extrinsicNavigationWrapper {

    private val giftAccount = createGiftInteractor.randomGiftAccount()

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

    val giftAccountFlow = chainFlow.map { chain ->
        createAddressModel(
            address = chain.addressOf(giftAccount.account),
            chain = chain
        )
    }
        .inBackground()
        .share()

    private val feeFormatter = GiftFeeDisplayFormatter(amountFormatter)
    val feeMixin = feeLoaderMixinFactory.createForGifts(
        chainAssetFlow,
        feeFormatter
    )

    val totalAmountModel = combine(assetFlow, feeMixin.fee) { asset, fee ->
        val feeAmount = fee.loadedFeeOrNull()?.amount
        if (feeAmount == null) {
            ExtendedLoadingState.Loading
        } else {
            amountFormatter.formatAmountToAmountModel(payload.amount, asset, AmountConfig(tokenAmountSign = AmountSign.NEGATIVE))
                .asLoaded()
        }
    }

    val giftAmountModel = assetFlow.map { asset ->
        amountFormatter.formatAmountToAmountModel(payload.amount, asset, AmountConfig(tokenAmountSign = AmountSign.NEGATIVE))
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

    fun confirmClicked() = launchUnit {
        validationInProgressFlow.value = true
        val fee = feeMixin.awaitFee()
        val chain = chainFlow.first()

        val giftModel = CreateGiftModel(
            metaAccount = selectedAccountUseCase.getSelectedMetaAccount(),
            chain = chain,
            chainAsset = chainAssetFlow.first(),
            giftAccount = giftAccount.account,
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
            performTransfer(validPayload.transfer, validPayload.originFee)
        }
    }

    private fun performTransfer(
        transfer: WeightedAssetTransfer,
        originFee: OriginFee
    ) = launch {
        sendUseCase.performTransfer(transfer, originFee.submissionFee, viewModelScope)
            .onSuccess {
                showToast(resourceManager.getString(io.novafoundation.nova.feature_assets.R.string.common_transaction_submitted))

                //TODO("Save gift in memory")

                startNavigation(it.submissionHierarchy) { finishCreateGift() }
            }.onFailure(::showError)

        validationInProgressFlow.value = false
    }

    private fun finishCreateGift() {
        showToast("Gift created. Next screen is not implemented")
    }

    private fun setupFees() {
        feeMixin.loadFee {
            val metaAccount = metaAccountFlow.first()
            val chain = chainFlow.first()

            val createGiftModel = CreateGiftModel(
                metaAccount = metaAccount,
                chain = chain,
                chainAsset = chainAssetFlow.first(),
                giftAccount = giftAccount.account,
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
