package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.validation.notValidOrNull
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakeRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakingDetails
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.message
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorFeeUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorValidatorUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorCommunicator
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setAmountInput
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
import kotlinx.coroutines.flow.flatMapLatest
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
    private val stakeValidationSystem: BittensorStakeValidationSystem
) : BaseViewModel() {

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

    private val feeResult = feeRequest.flatMapLatest { request ->
        if (request == null) {
            flowOf<FeeEstimate?> { null }
        } else {
            retryController.observe("root fee") {
                flowOf {
                    delay(FEE_DEBOUNCE_MILLIS)
                    bittensorStakingInteractor.estimateRootStakeFee(request)
                }
            }
                .map { FeeEstimate(request, it) }
        }
    }
        .shareInBackground()

    private val currentFee = combine(feeRequest, feeResult) { request, estimate ->
        estimate?.takeIf { it.request == request }
    }
        .shareInBackground()

    val amountModel = amountChooser.assetModel

    val fiatAmount = combine(stakingDetails, amount) { details, amount ->
        amount.toBigDecimalOrNull()
            ?.let(details.asset.token::toFiatOrNull)
            ?.let { formatFiat(it, details.asset.token.currency) }
    }
        .shareInBackground()

    val maxAmount = combine(stakingDetails, currentFee) { details, fee ->
        val available = maxAvailableInPlanks(details, fee?.state?.dataOrNull)
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

    val fee = combine(stakingDetails, currentFee) { details, result ->
        result?.state?.dataOrNull?.let { bittensorAmountFormatter.fee(it, details.asset.token) } ?: BittensorFeeUi("—", "—")
    }
        .shareInBackground()

    private val loadedContinueButtonState = combine(stakingDetails, amountInPlanks, feeResult, preparing) { details, amount, fee, preparing ->
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
        preparing.value = true
        try {
            val request = feeRequest.first() ?: return@launchUnit
            val details = stakingDetails.first()
            val fee = currentFee.first()?.state?.dataOrNull ?: return@launchUnit
            if (!validateStake(request, details, fee)) return@launchUnit
            bittensorStakingInteractor.prepareRootStake(request)
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

    private suspend fun validateStake(request: BittensorRootStakeRequest, details: BittensorRootStakingDetails, fee: Fee): Boolean {
        val amount = details.asset.token.amountFromPlanks(request.amountInPlanks)
        val payload = BittensorStakeValidationPayload(request.metaId, details.metaId, details.asset, amount, fee)
        val failure = stakeValidationSystem.validate(payload)
            .getOrThrow()
            .notValidOrNull() ?: return true

        showError(failure.reason.message(resourceManager))
        return false
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
        fee: FeeEstimate?
    ): DescriptiveButtonState {
        val label = resourceManager.getString(R.string.common_continue)
        val validator = details.validator ?: return DescriptiveButtonState.Disabled(resourceManager.getString(R.string.bittensor_select_validator))
        if (amount == null || !amount.isPositive()) {
            return DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_enter_amount))
        }

        if (fee?.request != BittensorRootStakeRequest(details.metaId, details.chain, validator, amount)) return DescriptiveButtonState.Loading
        if (fee.state is ExtendedLoadingState.Loading) return DescriptiveButtonState.Loading
        val loadedFee = fee.state.dataOrNull ?: return DescriptiveButtonState.Disabled(label)
        val enoughBalance = amount <= maxAvailableInPlanks(details, loadedFee)

        return if (enoughBalance) DescriptiveButtonState.Enabled(label) else DescriptiveButtonState.Disabled(label)
    }

    private fun maxAvailableInPlanks(details: BittensorRootStakingDetails, fee: Fee?): BigInteger {
        val feeInPlanks = fee?.maxAmountDeductionFor(details.asset.token.configuration) ?: BigInteger.ZERO
        return (details.asset.transferableInPlanks - feeInPlanks).coerceAtLeast(BigInteger.ZERO)
    }

    private fun formatToken(amount: BigDecimal, symbol: TokenSymbol): String =
        tokenFormatter.formatToken(amount, symbol)
            .toString()

    private fun formatFiat(amount: BigDecimal, currency: Currency): String {
        return fiatFormatter.formatFiat(amount, currency)
            .toString()
    }

    private data class FeeEstimate(val request: BittensorRootStakeRequest, val state: ExtendedLoadingState<Fee>)

    private companion object {
        const val FEE_DEBOUNCE_MILLIS = 350L

        val SUGGESTED_AMOUNT: BigDecimal = BigDecimal("5")
    }
}
