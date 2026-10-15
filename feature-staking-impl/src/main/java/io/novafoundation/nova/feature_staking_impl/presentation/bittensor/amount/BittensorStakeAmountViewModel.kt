package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakeRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakingDetails
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.maximumBittensorStake
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFeeEstimate
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.forRequest
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.message
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorValidatorUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorCommunicator
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setAmountInput
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.asFeeContextFromSelf
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefaultBy
import io.novafoundation.nova.runtime.ext.addressOf
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

class BittensorStakeAmountViewModel(
    private val resourceManager: ResourceManager,
    private val router: StakingRouter,
    private val bittensorStakingInteractor: BittensorStakingInteractor,
    private val iconGenerator: AddressIconGenerator,
    private val validatorCommunicator: BittensorSelectValidatorCommunicator,
    private val fiatFormatter: FiatFormatter,
    private val tokenFormatter: TokenFormatter,
    private val retryController: BittensorRetryController,
    private val bittensorAmountFormatter: BittensorAmountFormatter,
    private val amountChooserFactory: AmountChooserMixin.Factory,
    private val stakeValidationSystem: BittensorStakePreflightSystem,
    private val validationExecutor: ValidationExecutor,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory
) : BaseViewModel(), Validatable by validationExecutor {

    val retryEvents = retryController.events

    private val stakingDetailsState = retryController.observe("root staking details", bittensorStakingInteractor::observeRootStakingDetails)
        .shareInBackground()

    private val stakingDetails = stakingDetailsState.filterLoaded()
        .shareInBackground()

    private val amountChooser = amountChooserFactory.create(
        scope = this,
        assetFlow = stakingDetails.map { it.asset },
        maxActionProvider = null
    )

    val amount = amountChooser.inputState.map { it.value }
        .shareInBackground()

    private val preparing = MutableStateFlow(false)

    private var initializedMetaId: Long? = null

    private val amountInPlanks = combine(stakingDetails, amount) { details, amount ->
        amount.toBigDecimalOrNull()?.let(details.asset.token.configuration::planksFromAmount)
    }
        .distinctUntilChanged()
        .shareInBackground()

    private val feeRequest = combine(stakingDetails, amount, ::createFeeRequest)
        .distinctUntilChanged()
        .shareInBackground()

    private val feeContext = stakingDetails
        .map { it.asset.token.configuration }
        .asFeeContextFromSelf()

    val feeLoaderMixin = feeLoaderMixinFactory.createDefaultBy<BittensorFeeEstimate<BittensorRootStakeRequest>>(
        scope = this,
        feeContext = feeContext
    )

    val fee = combine(feeRequest, feeLoaderMixin.fee) { request, status ->
        status.forRequest(request)
    }
        .shareInBackground()

    val amountModel = amountChooser.assetModel

    val fiatAmount = combine(stakingDetails, amount) { details, amount ->
        amount.toBigDecimalOrNull()
            ?.let(details.asset.token::toFiatOrNull)
            ?.let { formatFiat(it, details.asset.token.currency) }
    }
        .shareInBackground()

    val maxAmount = combine(stakingDetails, fee) { details, fee ->
        val available = maxAvailableInPlanks(details, fee.loadedFeeOrNull())
        formatToken(details.asset.token.amountFromPlanks(available), details.asset.token.configuration.symbol)
    }
        .shareInBackground()

    val validator = stakingDetails
        .map {
            val validator = it.validator ?: return@map BittensorValidatorUi(resourceManager.getString(R.string.bittensor_select_validator), null)
            BittensorValidatorUi(
                validator.name,
                Icon.FromDrawable(
                    iconGenerator.createAddressIcon(
                        validator.hotkey.value,
                        AddressIconGenerator.SIZE_MEDIUM,
                        AddressIconGenerator.BACKGROUND_TRANSPARENT
                    )
                )
            )
        }
        .shareInBackground()

    val apy = stakingDetails
        .map { bittensorAmountFormatter.apy(it.annualPercentageYield, it.asset.token.configuration.symbol) }
        .shareInBackground()

    private val loadedContinueButtonState = combine(stakingDetails, amountInPlanks, fee, preparing) { details, amount, fee, preparing ->
        if (preparing) DescriptiveButtonState.Loading else continueButtonState(details, amount, fee)
    }

    val continueButtonState = combine(loadedContinueButtonState, stakingDetailsState) { button, state ->
        when (state) {
            is ExtendedLoadingState.Error -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_continue))
            is ExtendedLoadingState.Loading -> DescriptiveButtonState.Loading
            is ExtendedLoadingState.Loaded -> button
        }
    }
        .onStart { emit(DescriptiveButtonState.Loading) }
        .shareInBackground()

    init {
        feeLoaderMixin.connectWith(feeRequest) { _, request ->
            request?.let {
                delay(FEE_DEBOUNCE_MILLIS)
                val fee = bittensorStakingInteractor.estimateRootStakeFee(it)
                BittensorFeeEstimate(it, fee)
            }
        }

        stakingDetails
            .onEach(::setInitialAmount)
            .launchIn(viewModelScope)

        validatorCommunicator.responseFlow
            .filter { BittensorNetuid.from(it.subnetId).isRoot() }
            .onEach { selectValidator(it.address) }
            .launchIn(viewModelScope)
    }

    fun back() {
        bittensorStakingInteractor.clearDraft()
        router.back()
    }

    fun amountChanged(amount: String) {
        amountChooser.setAmountInput(amount, initiatedByUser = true)
    }

    fun maxClicked() = launchUnit {
        if (preparing.value) return@launchUnit
        preparing.value = true
        try {
            val details = stakingDetails.first()
            val maximum = bittensorStakingInteractor.maximumStake(details)
            amountChooser.setAmountInput(details.asset.token.amountFromPlanks(maximum).toStripTrailingZerosString())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showError(e)
        } finally {
            preparing.value = false
        }
    }

    fun validatorClicked() = launchUnit {
        if (preparing.value) return@launchUnit
        val details = stakingDetails.first()
        validatorCommunicator.openRequest(
            BittensorSelectValidatorCommunicator.Request(BittensorNetuid.Root.value, details.validator?.let { details.chain.addressOf(it.hotkey) })
        )
    }

    fun continueClicked() = launchUnit {
        if (preparing.value || continueButtonState.first() !is DescriptiveButtonState.Enabled) return@launchUnit
        val request = feeRequest.first() ?: return@launchUnit
        val fee = fee.first()
            .loadedFeeOrNull()
            ?.takeIf { it.request == request } ?: return@launchUnit
        preparing.value = true
        try {
            val payload = bittensorStakingInteractor.validationPayload(request, fee, checkRootHold = true)
            validationExecutor.requireValid(
                validationSystem = stakeValidationSystem,
                payload = payload,
                progressConsumer = preparing.progressConsumer(),
                validationFailureTransformer = { resourceManager.getString(R.string.common_error_general_title) to it.message(resourceManager) }
            ) { prepareStake(request, fee.fee) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            preparing.value = false
            showError(e)
        }
    }

    private fun prepareStake(request: BittensorRootStakeRequest, fee: Fee) = launchUnit {
        try {
            bittensorStakingInteractor.prepareRootStake(request, fee)
            router.openBittensorStakeConfirm()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showError(e)
        } finally {
            preparing.value = false
        }
    }

    private fun createFeeRequest(details: BittensorRootStakingDetails, input: String): BittensorRootStakeRequest? {
        val validator = details.validator ?: return null
        val amount = input.toBigDecimalOrNull()
            ?.let(details.asset.token.configuration::planksFromAmount) ?: return null
        if (!amount.isPositive() || amount.bitLength() > 64) return null

        return BittensorRootStakeRequest(details.metaId, details.chain, validator, amount)
    }

    private suspend fun selectValidator(address: String) {
        try {
            val details = stakingDetails.first()
            bittensorStakingInteractor.selectValidator(details, address)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showError(e)
        }
    }

    private fun setInitialAmount(details: BittensorRootStakingDetails) {
        if (initializedMetaId == details.metaId) return
        initializedMetaId = details.metaId
        if (amountChooser.inputState.value.value.isNotEmpty()) return

        val suggested = minOf(SUGGESTED_AMOUNT, details.asset.transferable)
        amountChooser.setAmountInput(suggested.toStripTrailingZerosString())
    }

    private fun continueButtonState(
        details: BittensorRootStakingDetails,
        amount: BigInteger?,
        fee: FeeStatus<BittensorFeeEstimate<BittensorRootStakeRequest>, FeeDisplay>
    ): DescriptiveButtonState {
        val label = resourceManager.getString(R.string.common_continue)
        val validator = details.validator ?: return DescriptiveButtonState.Disabled(resourceManager.getString(R.string.bittensor_select_validator))
        if (amount == null || !amount.isPositive()) {
            return DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_enter_amount))
        }

        if (fee is FeeStatus.Loading) return DescriptiveButtonState.Loading
        val loadedFee = fee.loadedFeeOrNull() ?: return DescriptiveButtonState.Disabled(label)
        if (loadedFee.request != BittensorRootStakeRequest(details.metaId, details.chain, validator, amount)) return DescriptiveButtonState.Loading
        val enoughBalance = amount <= maxAvailableInPlanks(details, loadedFee)

        return if (enoughBalance) DescriptiveButtonState.Enabled(label) else DescriptiveButtonState.Disabled(label)
    }

    private fun maxAvailableInPlanks(details: BittensorRootStakingDetails, fee: Fee?): BigInteger {
        val feeInPlanks = fee?.maxAmountDeductionFor(details.asset.token.configuration) ?: BigInteger.ZERO
        return maximumBittensorStake(details.asset.transferableInPlanks, feeInPlanks)
    }

    private fun formatToken(amount: BigDecimal, symbol: TokenSymbol): String =
        tokenFormatter.formatToken(amount, symbol)
            .toString()

    private fun formatFiat(amount: BigDecimal, currency: Currency): String {
        return fiatFormatter.formatFiat(amount, currency)
            .toString()
    }

    private companion object {
        const val FEE_DEBOUNCE_MILLIS = 350L

        val SUGGESTED_AMOUNT: BigDecimal = BigDecimal("5")
    }
}
