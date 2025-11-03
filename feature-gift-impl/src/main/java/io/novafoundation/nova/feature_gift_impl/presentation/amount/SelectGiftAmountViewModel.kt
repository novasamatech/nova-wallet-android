package io.novafoundation.nova.feature_gift_impl.presentation.amount

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.CompoundFieldValidator
import io.novafoundation.nova.common.validation.FieldValidationResult
import io.novafoundation.nova.common.validation.FieldValidator
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.isErrorWithTag
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.view.ChainChipModel
import io.novafoundation.nova.feature_assets.presentation.send.autoFixSendValidationPayload
import io.novafoundation.nova.feature_assets.presentation.send.mapAssetTransferValidationFailureToUI
import io.novafoundation.nova.feature_gift_impl.R
import io.novafoundation.nova.feature_gift_impl.domain.CreateGiftInteractor
import io.novafoundation.nova.feature_gift_impl.domain.models.CreateGiftModel
import io.novafoundation.nova.feature_gift_impl.domain.models.GiftFee
import io.novafoundation.nova.feature_gift_impl.presentation.GiftRouter
import io.novafoundation.nova.feature_gift_impl.presentation.amount.fee.createForGiftsWithDefaultDisplay
import io.novafoundation.nova.feature_gift_impl.presentation.common.buildGiftValidationPayload
import io.novafoundation.nova.feature_gift_impl.presentation.confirm.CreateGiftConfirmPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.common.fieldValidator.EnoughAmountFieldValidator
import io.novafoundation.nova.feature_wallet_api.presentation.common.fieldValidator.EnoughAmountValidatorFactory
import io.novafoundation.nova.feature_wallet_api.presentation.common.fieldValidator.MinAmountFieldValidatorFactory
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.isMaxAction
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.formatter.DefaultFeeFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset.GetAssetOptionsMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.create
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chainFlow
import java.math.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class SelectGiftAmountViewModel(
    private val router: GiftRouter,
    private val chainRegistry: ChainRegistry,
    private val validationExecutor: ValidationExecutor,
    private val assetUseCase: ArbitraryAssetUseCase,
    private val payload: SelectGiftAmountPayload,
    private val maxActionProviderFactory: MaxActionProviderFactory,
    private val amountFormatter: AmountFormatter,
    private val resourceManager: ResourceManager,
    private val getAssetOptionsMixinFactory: GetAssetOptionsMixin.Factory,
    private val createGiftInteractor: CreateGiftInteractor,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val enoughAmountValidatorFactory: EnoughAmountValidatorFactory,
    private val minAmountFieldValidatorFactory: MinAmountFieldValidatorFactory,
    private val giftMinAmountProviderFactory: GiftMinAmountProviderFactory,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
) : BaseViewModel(),
    Validatable by validationExecutor {

    private val chainFlow = chainRegistry.chainFlow(payload.assetPayload.chainId)
    private val chainAssetFlow = chainFlow.map { it.assetsById.getValue(payload.assetPayload.chainAssetId) }

    private val metaAccountFlow = selectedAccountUseCase.selectedMetaAccountFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val assetFlow = chainAssetFlow.flatMapLatest(assetUseCase::assetFlow)
        .shareInBackground()

    val chainModelFlow = chainFlow.map {
        ChainChipModel(
            chainUi = mapChainToUi(it),
            changeable = false
        )
    }.shareInBackground()

    private val feeFormatter = DefaultFeeFormatter<GiftFee>(amountFormatter)
    val feeMixin = feeLoaderMixinFactory.createForGiftsWithDefaultDisplay(
        chainAssetFlow,
        feeFormatter
    )

    private val maxActionProvider = maxActionProviderFactory.create(
        viewModelScope = viewModelScope,
        assetInFlow = assetFlow,
        feeLoaderMixin = feeMixin
    )

    val amountChooserMixin: AmountChooserMixin.Presentation = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = assetFlow,
        maxActionProvider = maxActionProvider,
        fieldValidator = getAmountValidator()
    )

    private val notEnoughAmountErrorFlow = combine(assetFlow, amountChooserMixin.fieldError) { asset, fieldError ->
        asset.transferable.isZero || fieldError.isErrorWithTag(EnoughAmountFieldValidator.ERROR_TAG)
    }
    val getAssetOptionsMixin = getAssetOptionsMixinFactory.create(
        assetFlow = chainAssetFlow,
        additionalButtonFilter = notEnoughAmountErrorFlow,
        scope = viewModelScope,
    )

    private val validationInProgressFlow = MutableStateFlow(false)

    val continueButtonStateFlow = combine(
        validationInProgressFlow,
        amountChooserMixin.fieldError,
        amountChooserMixin.inputState
    ) { validating, fieldError, amountState ->
        when {
            validating -> DescriptiveButtonState.Loading
            fieldError is FieldValidationResult.Error -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_enter_other_amount))
            amountState.value.isNotEmpty() -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
            else -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.gift_enter_amount_disabled_button_state))
        }
    }.onStart { emit(DescriptiveButtonState.Disabled(resourceManager.getString(R.string.gift_enter_amount_disabled_button_state))) }

    init {
        setupFees()
    }

    fun back() {
        router.back()
    }

    fun nextClicked() = launchUnit {
        validationInProgressFlow.value = true
        val fee = feeMixin.awaitFee()
        val amountState = amountChooserMixin.amountState.first()
        val giftAmount = amountState.value ?: return@launchUnit
        val chain = chainFlow.first()

        val giftModel = CreateGiftModel(
            senderMetaAccount = selectedAccountUseCase.getSelectedMetaAccount(),
            chain = chain,
            chainAsset = chainAssetFlow.first(),
            amount = giftAmount,
        )

        val payload = buildGiftValidationPayload(
            giftModel,
            asset = assetFlow.first(),
            amountState.inputKind.isMaxAction(),
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
        ) {
            validationInProgressFlow.value = false

            openConfirmScreen(it, giftAmount)
        }
    }

    private fun openConfirmScreen(validPayload: AssetTransferPayload, giftAmount: BigDecimal) = launch {
        val payload = CreateGiftConfirmPayload(
            amount = giftAmount,
            transferringMaxAmount = validPayload.transfer.transferringMaxAmount,
            assetPayload = payload.assetPayload
        )

        router.openConfirmCreateGift(payload)
    }

    private fun setupFees() {
        feeMixin.connectWith(
            chainFlow,
            chainAssetFlow,
            amountChooserMixin.amountState,
        ) { feePaymentCurrency, chain, chainAsset, amountState ->
            val metaAccount = metaAccountFlow.first()
            val createGiftModel = CreateGiftModel(
                senderMetaAccount = metaAccount,
                chain = chain,
                chainAsset = chainAsset,
                amount = amountState.value.orZero(),
            )

            createGiftInteractor.getFee(
                createGiftModel,
                amountState.inputKind.isMaxAction(),
                viewModelScope
            )
        }
    }

    private fun getAmountValidator(): FieldValidator {
        val minAmountProvider = giftMinAmountProviderFactory.create(chainAssetFlow)

        return CompoundFieldValidator(
            enoughAmountValidatorFactory.create(maxActionProvider),
            minAmountFieldValidatorFactory.create(minAmountProvider, R.string.gift_min_balance_validation_message)
        )
    }
}
