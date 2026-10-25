package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorStakeAlertFormat
import io.novafoundation.nova.analytics.StakingValidationStage
import io.novafoundation.nova.analytics.StakingValidationOutcome
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.analyticsKind
import io.novafoundation.nova.analytics.StakingBlockedReason
import io.novafoundation.nova.analytics.StakingFlow
import io.novafoundation.nova.analytics.BittensorDataEndpoint
import io.novafoundation.nova.analytics.BittensorScreen
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorReadAnalytics
import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.analytics.StakingMode
import io.novafoundation.nova.analytics.StakingValidatorSource
import io.novafoundation.nova.analytics.AnalyticsService

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.presentation.ellipsizeAddress
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakeRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakingDetails
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootValidatorDetails
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.bittensorMonthlyProjection
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.maximumBittensorStake
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFeeEstimate
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorMaxAmount
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorNeedsFunding
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.forRequest
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.titleAndMessage
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.BittensorStakeConfirmPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorValidatorUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectValidatorCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectionState
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.actualAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.create
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setAmountInput
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.asFeeContextFromSelf
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefaultBy
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset.GetAssetOptionsMixin
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

class BittensorStakeAmountViewModel(
    private val analytics: AnalyticsService,
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
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    private val externalActions: ExternalActions.Presentation,
    getAssetOptionsMixinFactory: GetAssetOptionsMixin.Factory,
    private val payload: BittensorStakeAmountPayload? = null,
    initialSelectionState: BittensorSelectionState = BittensorSelectionState()
) : BaseViewModel(), Validatable by validationExecutor, ExternalActions by externalActions {

    private var selectionState = initialSelectionState

    private var isNoBalanceTracked = false

    /** Set once the setup moves on to the confirmation. */
    private var flowContinued = false

    private val rootOverviewReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.ROOT_OVERVIEW, BittensorScreen.SETUP)
    private val validatorsReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.VALIDATORS, BittensorScreen.SETUP)

    val retryEvents = retryController.events

    /**
     * Adding to an existing root position is titled "Add stake to root", and the Where to stake root card or a quick pick
     * switched to root "Earn with Bittensor".
     */
    val titleRes = when {
        payload?.validatorAddress != null -> R.string.bittensor_add_stake_to_root
        payload?.quickPick == true || initialSelectionState.quickPick -> R.string.bittensor_earn_title
        else -> R.string.bittensor_stake_to_root
    }

    private val stakingDetailsState = retryController.observe(
        step = "root staking details",
        analytics = rootOverviewReads,
        transform = { it.withSafeLoading() },
        source = ::observeStakingDetails
    )
        .shareInBackground()

    private val selectedValidator = MutableStateFlow<SelectedValidator?>(null)

    private val fixedValidatorState = observeFixedValidator()
        .shareInBackground()

    private val selectedStakingDetailsState = combine(
        stakingDetailsState,
        selectedValidator,
        fixedValidatorState
    ) { state, selected, fixed ->
        state.map { details ->
            val fixedAddress = payload?.validatorAddress
            when {
                fixedAddress != null -> bittensorStakingInteractor.detailsForFixedValidator(details, fixedAddress, fixed.dataOrNull)
                selected?.metaId == details.metaId -> bittensorStakingInteractor.detailsForValidator(details, selected.validator)
                else -> details
            }
        }
    }
        .shareInBackground()

    private val stakingDetails = selectedStakingDetailsState.filterLoaded()
        .shareInBackground()

    private val feeContext = stakingDetails
        .map { it.asset.token.configuration }
        .asFeeContextFromSelf()

    val feeLoaderMixin = feeLoaderMixinFactory.createDefaultBy<BittensorFeeEstimate<BittensorRootStakeRequest>>(
        scope = this,
        feeContext = feeContext
    )

    private val maxActionProvider = MaxActionProvider.create(this) {
        stakingDetails.map { it.asset }
            .providingMaxOf { maximumBittensorStake(it.transferableInPlanks, BigInteger.ZERO) }
            .deductFee(feeLoaderMixin)
    }

    private val amountChooser = amountChooserFactory.create(
        scope = this,
        assetFlow = stakingDetails.map { it.asset },
        maxActionProvider = maxActionProvider
    )

    val amount = amountChooser.inputState.map { it.value }
        .shareInBackground()

    private val preparing = MutableStateFlow(false)

    private val amountInPlanks = combine(
        stakingDetails,
        amount
    ) { details, amount ->
        amount.toBigDecimalOrNull()
            ?.let(details.asset.token.configuration::planksFromAmount)
    }
        .distinctUntilChanged()
        .shareInBackground()

    private val feeRequest = combine(stakingDetails, amount, ::createFeeRequest)
        .distinctUntilChanged()
        .shareInBackground()

    val fee = combine(
        feeRequest,
        feeLoaderMixin.fee
    ) { request, status ->
        status.forRequest(request)
    }
        .shareInBackground()

    val amountModel = amountChooser.assetModel

    val needsFunding = combine(
        selectedStakingDetailsState,
        fee
    ) { state, fee ->
        val details = state.dataOrNull
        val networkFee = details?.let { fee.loadedFeeOrNull()?.maxAmountDeductionFor(it.asset.token.configuration) }
        bittensorNeedsFunding(details?.asset?.transferableInPlanks, networkFee, locked = payload?.metaId != null)
    }
        .distinctUntilChanged()
        .shareInBackground()

    val getAssetOptionsMixin = getAssetOptionsMixinFactory.create(
        assetFlow = selectedStakingDetailsState.map { it.dataOrNull?.asset?.token?.configuration },
        scope = this,
        additionalButtonFilter = needsFunding,
        showWhenNoOptions = true
    )

    val fundingMessage = stakingDetails.map { details ->
        val symbol = details.asset.token.configuration.symbol
        val balance = tokenFormatter.formatToken(details.asset.transferable, symbol)
        resourceManager.getString(R.string.bittensor_get_asset_message, balance, symbol.value)
    }
        .shareInBackground()

    val fiatAmount = combine(
        stakingDetails,
        amount
    ) { details, amount ->
        amount.toBigDecimalOrNull()
            ?.let(details.asset.token::toFiatOrNull)
            ?.let { formatFiat(it, details.asset.token.currency) }
    }
        .shareInBackground()

    val addStakeCaption = combine(selectedStakingDetailsState, needsFunding) { state, funding ->
        val details = state.dataOrNull ?: return@combine null
        val stake = details.position?.stakeInPlanks?.takeIf { it.isPositive() } ?: return@combine null
        val validator = details.validator ?: return@combine null
        if (payload?.metaId == null || funding || details.dataStatus !is BittensorDataStatus.Fresh) return@combine null
        val name = validator.name.ifBlank { details.chain.addressOf(validator.hotkey).ellipsizeAddress() }
        val amount = formatToken(details.asset.token.amountFromPlanks(stake), details.asset.token.configuration.symbol)
        resourceManager.getString(R.string.bittensor_adds_to_position, amount, name)
    }
        .shareInBackground()

    val reserveWarning = combine(stakingDetails, amount, fee, needsFunding) { details, input, status, funding ->
        if (funding) null else bittensorAmountFormatter.feeReserveWarning(input, details.asset, status.loadedFeeOrNull())
    }
        .shareInBackground()

    val monthlyEstimate = combine(
        selectedStakingDetailsState,
        amount
    ) { state, text ->
        state.map { details ->
            val annual = details.annualPercentageYield ?: return@map "—"
            val principal = text.toBigDecimalOrNull()
                ?.takeIf { it.isPositive } ?: return@map "—"
            val estimate = bittensorMonthlyProjection(principal, annual)
            "≈ " + formatToken(estimate, details.asset.token.configuration.symbol)
        }
    }
        .shareInBackground()

    private val maximumAmount = maxActionProvider.maxAvailableBalance
        .map { BittensorMaxAmount(it.actualAmount, it.chainAsset.symbol) }
        .shareInBackground()

    val maxAmount = maximumAmount.map { formatToken(it.amount, it.symbol) }
        .shareInBackground()

    val validator = stakingDetails
        .map { details ->
            val validator = details.validator
            val fixedAddress = payload?.validatorAddress
            val hotkey = validator?.hotkey?.value ?: fixedAddress?.let(details.chain::accountIdOf)
                ?: return@map BittensorValidatorUi(resourceManager.getString(R.string.bittensor_select_validator_title), null)
            val name = validator?.name ?: requireNotNull(fixedAddress).ellipsizeAddress()
            BittensorValidatorUi(
                name,
                Icon.FromDrawable(
                    iconGenerator.createAddressIcon(
                        hotkey,
                        AddressIconGenerator.SIZE_MEDIUM,
                        AddressIconGenerator.BACKGROUND_TRANSPARENT
                    )
                )
            )
        }
        .shareInBackground()

    val apy = selectedStakingDetailsState
        .mapLoading { bittensorAmountFormatter.apy(it.annualPercentageYield, it.asset.token.configuration.symbol) }
        .shareInBackground()

    private val loadedContinueButtonState = combine(
        stakingDetails,
        amountInPlanks,
        fee,
        preparing
    ) { details, amount, fee, preparing ->
        if (preparing) DescriptiveButtonState.Loading else continueButtonState(details, amount, fee)
    }

    val continueButtonState = combine(
        loadedContinueButtonState,
        stakingDetailsState,
        needsFunding,
        fixedValidatorState
    ) { button, state, needsFunding, fixed ->
        when {
            needsFunding -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_continue))
            state is ExtendedLoadingState.Error || fixed is ExtendedLoadingState.Error ->
                DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_continue))
            state is ExtendedLoadingState.Loading || fixed is ExtendedLoadingState.Loading -> DescriptiveButtonState.Loading
            else -> button
        }
    }
        .onStart { emit(DescriptiveButtonState.Loading) }
        .shareInBackground()

    init {
        needsFunding.onEach { funding ->
            if (funding && !isNoBalanceTracked) {
                isNoBalanceTracked = true
                analytics.track(
                    BittensorAnalyticsEvent.Blocked(
                        StakingFlow.STAKE,
                        StakingBlockedReason.NO_BALANCE,
                        null,
                        StakingAnalyticsTarget.ROOT
                    )
                )
            }
        }.launchIn(this)
        feeLoaderMixin.connectWith(feeRequest) { _, request ->
            request?.let {
                delay(FEE_DEBOUNCE_MILLIS)
                val fee = bittensorStakingInteractor.estimateRootStakeFee(it)
                BittensorFeeEstimate(it, fee)
            }
        }

        validatorCommunicator.responseFlow
            .onEach { response ->
                if (!selectionState.accepts(response)) return@onEach
                selectionState = selectionState.withResponse(response)
                if (payload?.validatorAddress == null && response.subnetId == BittensorNetuid.Root.value) {
                    response.address?.let { selectValidator(it) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun back() {
        // Leaving the setup of a new position before continuing reports it abandoned. A setup replaced by the
        // subnet setup is not left: the flow keeps one setup and only changes its target.
        if (!flowContinued && payload?.validatorAddress == null) {
            analytics.track(BittensorAnalyticsEvent.Abandoned(BittensorScreen.SETUP, StakingAnalyticsTarget.ROOT))
        }
        validatorCommunicator.respond(BittensorSelectValidatorCommunicator.Response(null, null, selectionState))
        router.back()
    }

    fun amountChanged(amount: String) {
        if (amountChooser.inputState.value.value == amount) return
        amountChooser.setAmountInput(amount, initiatedByUser = true)
    }

    fun maxClicked() = launchUnit {
        if (preparing.value) return@launchUnit
        amountChooser.maxAction.maxClick.first()?.invoke()
    }

    fun validatorClicked() = launchUnit {
        if (preparing.value) return@launchUnit
        val details = stakingDetails.first()
        val fixedAddress = payload?.validatorAddress
        if (fixedAddress != null) {
            externalActions.showAddressActions(fixedAddress, details.chain)
            return@launchUnit
        }
        // The root preset is the recommended validator, so confirming it again is not a manual change.
        val preset = stakingDetailsState.first().dataOrNull?.validator
        validatorCommunicator.openRequest(
            BittensorSelectValidatorCommunicator.Request(
                BittensorNetuid.Root.value,
                details.validator?.let { details.chain.addressOf(it.hotkey) },
                selectionState,
                recommendedAddress = preset?.let { details.chain.addressOf(it.hotkey) }
            )
        )
    }

    fun continueClicked() = launchUnit {
        if (preparing.value || continueButtonState.first() !is DescriptiveButtonState.Enabled) return@launchUnit
        val request = feeRequest.first() ?: return@launchUnit
        val fee = fee.first()
            .loadedFeeOrNull()
            ?.takeIf { it.request == request } ?: return@launchUnit
        val token = stakingDetails.first().asset.token
        preparing.value = true
        try {
            val payload = bittensorStakingInteractor.validationPayload(request, fee)
            validationExecutor.requireValid(
                validationSystem = stakeValidationSystem,
                payload = payload,
                progressConsumer = preparing.progressConsumer(),
                validationFailureTransformer = { failure ->
                    failure.analyticsKind()?.let { kind ->
                        analytics.track(
                            BittensorAnalyticsEvent.ValidationShown(
                                StakingFlow.STAKE,
                                kind,
                                StakingValidationStage.SETUP,
                                StakingValidationOutcome.BLOCKED,
                                StakingAnalyticsTarget.ROOT
                            )
                        )
                    }
                    failure.titleAndMessage(resourceManager, tokenFormatter.bittensorStakeAlertFormat(token))
                }
            ) { openConfirmation(request) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            preparing.value = false
            showError(e)
        }
    }

    private fun observeStakingDetails(): Flow<BittensorRootStakingDetails> {
        val metaId = payload?.metaId ?: return bittensorStakingInteractor.observeRootStakingDetails()
        return when (payload.validatorAddress) {
            null -> bittensorStakingInteractor.observeRootStakingDetails(metaId)
            else -> bittensorStakingInteractor.observeRootAccountDetails(metaId)
        }
    }

    private fun observeFixedValidator(): Flow<ExtendedLoadingState<BittensorRootValidatorDetails?>> {
        val address = payload?.validatorAddress
            ?: return kotlinx.coroutines.flow.flowOf(ExtendedLoadingState.Loaded(null))
        return retryController.observe(
            step = "root position validator",
            analytics = validatorsReads,
            transform = { it.withSafeLoading() }
        ) {
            flowOf {
                val details = stakingDetailsState.filterLoaded().first()
                bittensorStakingInteractor.rootValidatorDetails(details.chain, address)
            }
        }
    }

    private fun openConfirmation(request: BittensorRootStakeRequest) {
        flowContinued = true
        val payload = BittensorStakeConfirmPayload(
            metaId = request.metaId,
            amountInPlanks = request.amountInPlanks,
            validatorAddress = request.chain.addressOf(request.validator.hotkey),
            validatorName = request.validator.name,
            existingPosition = this.payload?.validatorAddress != null
        )
        analytics.track(
            BittensorAnalyticsEvent.Initiated(
                BittensorAnalyticsEvent.NETWORK,
                StakingAnalyticsTarget.ROOT,
                if (payload.existingPosition) StakingMode.ADD_STAKE else StakingMode.NEW_POSITION,
                when {
                    payload.existingPosition -> StakingValidatorSource.EXISTING_HOTKEY
                    selectedValidator.value?.validator?.hotkey?.let { selected ->
                        selected != stakingDetailsState.replayCache.lastOrNull()?.dataOrNull?.validator?.hotkey
                    } == true -> StakingValidatorSource.MANUAL
                    else -> StakingValidatorSource.RECOMMENDED
                },
                null,
                null
            )
        )
        router.openBittensorStakeConfirm(payload)
        preparing.value = false
    }

    private fun createFeeRequest(details: BittensorRootStakingDetails, input: String): BittensorRootStakeRequest? {
        val validator = details.validator ?: return null
        val value = input.toBigDecimalOrNull()?.takeIf { it.isPositive } ?: BigDecimal.ONE
        val amount = details.asset.token.configuration.planksFromAmount(value)
        if (!amount.isPositive() || amount.bitLength() > 64) return null

        return BittensorRootStakeRequest(
            details.metaId,
            details.chain,
            validator,
            amount
        )
    }

    private suspend fun selectValidator(address: String) {
        try {
            val details = stakingDetails.first()
            val validator = bittensorStakingInteractor.rootValidator(details.chain, address)
            selectedValidator.value = SelectedValidator(details.metaId, validator)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showError(e)
        }
    }

    private fun continueButtonState(
        details: BittensorRootStakingDetails,
        amount: BigInteger?,
        fee: FeeStatus<BittensorFeeEstimate<BittensorRootStakeRequest>, FeeDisplay>
    ): DescriptiveButtonState {
        val label = resourceManager.getString(R.string.common_continue)
        // The root button never asks for a validator: it reads "Enter amount" until an amount is entered.
        if (amount == null || !amount.isPositive()) {
            return DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_enter_amount))
        }
        val validator = details.validator ?: return DescriptiveButtonState.Disabled(label)

        if (fee is FeeStatus.Loading) return DescriptiveButtonState.Loading
        val loadedFee = fee.loadedFeeOrNull() ?: return DescriptiveButtonState.Disabled(label)
        val currentRequest = BittensorRootStakeRequest(details.metaId, details.chain, validator, amount)
        if (loadedFee.request != currentRequest) {
            return DescriptiveButtonState.Loading
        }
        val enoughBalance = amount <= maxAvailableInPlanks(details, loadedFee)

        return if (enoughBalance) DescriptiveButtonState.Enabled(label) else DescriptiveButtonState.Disabled(label)
    }

    private fun maxAvailableInPlanks(details: BittensorRootStakingDetails, fee: Fee?): BigInteger {
        val feeInPlanks = fee?.maxAmountDeductionFor(details.asset.token.configuration) ?: BigInteger.ZERO
        return maximumBittensorStake(details.asset.transferableInPlanks, feeInPlanks)
    }

    private fun formatToken(amount: BigDecimal, symbol: TokenSymbol): String =
        tokenFormatter.formatToken(amount, symbol).toString()

    private fun formatFiat(amount: BigDecimal, currency: Currency): String {
        return fiatFormatter.formatFiat(amount, currency).toString()
    }

    private data class SelectedValidator(val metaId: Long, val validator: BittensorValidator)

    private companion object {
        const val FEE_DEBOUNCE_MILLIS = 350L
    }
}
