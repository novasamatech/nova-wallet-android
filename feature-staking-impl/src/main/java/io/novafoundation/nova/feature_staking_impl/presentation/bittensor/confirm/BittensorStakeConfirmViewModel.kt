package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorStakeAlertFormat
import java.math.BigInteger
import io.novafoundation.nova.analytics.StakingValidationStage
import io.novafoundation.nova.analytics.StakingValidationOutcome
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.analyticsKind
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.reportWatchOnlyStake
import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.BittensorDataEndpoint
import io.novafoundation.nova.analytics.BittensorScreen
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorReadAnalytics
import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.analytics.StakingMode
import io.novafoundation.nova.analytics.StakingFlow
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorOperationAnalytics

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationOutcome
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.formatNetworkFee
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mapBittensorPreSubmissionError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mapBittensorSubmissionError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakeRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakingDetails
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFeeEstimate
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorResultNavigation
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.forRequest
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.titleAndMessage
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.mapResult
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.toSubmissionState
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorResultAction
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorSubmissionState
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorValidatorUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.BittensorValidatorInfoPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.asFeeContextFromSelf
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefaultBy
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class BittensorStakeConfirmViewModel(
    private val analytics: AnalyticsService,
    private val operationAnalytics: BittensorOperationAnalytics,
    private val payload: BittensorStakeConfirmPayload,
    private val resourceManager: ResourceManager,
    private val router: StakingRouter,
    private val externalActions: ExternalActions.Presentation,
    chainRegistry: ChainRegistry,
    walletUiUseCase: WalletUiUseCase,
    accountRepository: AccountRepository,
    private val bittensorStakingInteractor: BittensorStakingInteractor,
    private val iconGenerator: AddressIconGenerator,
    private val fiatFormatter: FiatFormatter,
    private val tokenFormatter: TokenFormatter,
    private val bittensorAmountFormatter: BittensorAmountFormatter,
    private val retryController: BittensorRetryController,
    private val validationSystem: BittensorStakePreflightSystem,
    private val validationExecutor: ValidationExecutor,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
) : BaseViewModel(), ExternalActions by externalActions, Validatable by validationExecutor {

    private val chain by lazyAsync { chainRegistry.getChain(ChainGeneses.BITTENSOR) }

    private val rootOverviewReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.ROOT_OVERVIEW, BittensorScreen.CONFIRM)

    val retryEvents = retryController.events

    /** Confirming an add to an existing root position is titled "Add stake to root". */
    val titleRes = if (payload.existingPosition) R.string.bittensor_add_stake_to_root else R.string.bittensor_stake_to_root

    private val stakeRequest = flowOf { payload.toStakeRequest(chain.await()) }
        .shareInBackground()

    private val stakingDetailsState = retryController.observe(
        step = "root confirmation",
        analytics = rootOverviewReads,
        transform = { it.withSafeLoading() }
    ) {
        bittensorStakingInteractor.observeRootStakingDetails(payload.metaId)
    }
        .shareInBackground()

    private val selectedStakingDetailsState = combine(
        stakingDetailsState,
        stakeRequest
    ) { state, request ->
        state.map { bittensorStakingInteractor.detailsForValidator(it, request.validator) }
    }
        .shareInBackground()

    private val stakingDetails = selectedStakingDetailsState.filterLoaded()
        .shareInBackground()

    val amount = combine(stakingDetails, stakeRequest, ::mapAmount)
        .shareInBackground()

    val wallet = walletUiUseCase.walletUiFlow(payload.metaId)
        .shareInBackground()

    val account = accountRepository.metaAccountFlow(payload.metaId)
        .map { walletUiUseCase.walletAddressModel(it, chain.await(), AddressIconGenerator.SIZE_MEDIUM) }
        .shareInBackground()

    private val feeContext = stakingDetails
        .map { it.asset.token.configuration }
        .asFeeContextFromSelf()

    val feeLoaderMixin = feeLoaderMixinFactory.createDefaultBy<BittensorFeeEstimate<BittensorRootStakeRequest>>(
        scope = this,
        feeContext = feeContext
    )

    val fee = combine(
        stakeRequest,
        feeLoaderMixin.fee
    ) { request, status ->
        status.forRequest(request)
    }
        .shareInBackground()

    /** The root stake before this operation, taken when Confirm hands it off, so a refresh after the stake lands does not count it twice. */
    private var stakeBeforeSnapshot: BigInteger? = null

    /** Null hides "Stake after": it is shown only when adding to a position whose positions are fresh (iOS `createStakeChange`). */
    val stakeAfter = combine(
        selectedStakingDetailsState,
        stakeRequest
    ) { state, request ->
        state.map { details ->
            if (!bittensorShowsRootStakeAfter(payload.existingPosition, details.dataStatus)) return@map null
            val before = stakeBeforeSnapshot ?: bittensorRootStakeChange(details.positions, request.validator.hotkey, request.amountInPlanks).before
            val token = details.asset.token
            formatToken(token.amountFromPlanks(before), token.configuration.symbol) + " → " +
                formatToken(token.amountFromPlanks(before + request.amountInPlanks), token.configuration.symbol)
        }
    }
        .shareInBackground()

    val stakingType = resourceManager.getString(R.string.bittensor_root_staking)

    val validator = stakeRequest.map(::mapValidator)
        .shareInBackground()

    val apy = selectedStakingDetailsState.mapLoading { bittensorAmountFormatter.apy(it.annualPercentageYield, it.asset.token.configuration.symbol) }
        .shareInBackground()

    private val submission = MutableStateFlow<BittensorSubmissionState>(BittensorSubmissionState.Ready)

    val resultState = submission.map { it.result }
        .shareInBackground()

    private val validating = MutableStateFlow(false)

    /** Set once the stake is confirmed, also when it then fails. */
    private var flowContinued = false

    val confirmButtonState = combine(
        stakingDetailsState,
        validating,
        submission,
        fee
    ) { state, validating, submission, fee ->
        val details = state.dataOrNull
        val label = resourceManager.getString(R.string.common_confirm)
        if (!submission.canSubmit || details == null || validating || fee.loadedFeeOrNull() == null) {
            DescriptiveButtonState.Disabled(label)
        } else {
            DescriptiveButtonState.Enabled(label)
        }
    }
        .onStart { emit(DescriptiveButtonState.Loading) }
        .shareInBackground()

    init {
        feeLoaderMixin.connectWith(stakeRequest) { _, request ->
            val fee = bittensorStakingInteractor.estimateRootStakeFee(request)
            BittensorFeeEstimate(request, fee)
        }
        launch {
            // A watch-only wallet is reported blocked once the confirmation opens, and leaving it is not an abandonment.
            if (accountRepository.getMetaAccount(payload.metaId).type == LightMetaAccount.Type.WATCH_ONLY) {
                flowContinued = true
                analytics.reportWatchOnlyStake(StakingAnalyticsTarget.ROOT)
            }
        }
    }

    fun back() {
        when {
            submission.value is BittensorSubmissionState.Submitting -> Unit
            submission.value.isFinished -> resultNavigation.leave()
            else -> {
                // Leaving the confirmation of a new position before confirming reports it abandoned.
                if (!flowContinued && !payload.existingPosition) {
                    analytics.track(BittensorAnalyticsEvent.Abandoned(BittensorScreen.CONFIRM, StakingAnalyticsTarget.ROOT))
                }
                router.back()
            }
        }
    }

    fun accountClicked() = launchUnit {
        externalActions.showAddressActions(
            account.first().address,
            chain.await()
        )
    }

    fun validatorClicked() {
        router.openBittensorValidatorInfo(BittensorValidatorInfoPayload(BittensorNetuid.Root.value, payload.validatorAddress))
    }

    fun confirmClicked() = launchUnit {
        if (!submission.value.canSubmit || validating.value || stakingDetailsState.first().dataOrNull == null) {
            return@launchUnit
        }
        validating.value = true
        try {
            val request = stakeRequest.first()
            val fee = fee.first()
                .loadedFeeOrNull()
                ?.takeIf { it.request == request } ?: return@launchUnit
            val payload = bittensorStakingInteractor.validationPayload(request, fee)
            val token = stakingDetails.first().asset.token
            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                errorDisplayer = ::showConfirmError,
                progressConsumer = validating.progressConsumer(),
                scope = this@BittensorStakeConfirmViewModel,
                validationFailureTransformerDefault = { failure ->
                    failure.analyticsKind()?.let { kind ->
                        analytics.track(
                            BittensorAnalyticsEvent.ValidationShown(
                                StakingFlow.STAKE,
                                kind,
                                StakingValidationStage.CONFIRM,
                                StakingValidationOutcome.BLOCKED,
                                StakingAnalyticsTarget.ROOT
                            )
                        )
                    }
                    failure.titleAndMessage(resourceManager, tokenFormatter.bittensorStakeAlertFormat(token))
                }
            ) { executeStake(request, fee.fee) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            submission.value = e.mapBittensorPreSubmissionError().mapResult(resourceManager, resultNavigation.landingActions()).toSubmissionState()
        } finally {
            validating.value = false
        }
    }

    // A slow node gets the same Retry / Skip dialog as a failed read, not the raw "<read> timed out" text.
    private fun showConfirmError(error: Throwable) {
        if (!retryController.reportSlowAction("root stake confirm", error, ::confirmClicked)) showError(error)
    }

    private val resultNavigation = BittensorResultNavigation(
        router,
        analytics,
        StakingFlow.STAKE,
        StakingAnalyticsTarget.ROOT,
        newPosition = !payload.existingPosition
    )

    private val analyticsMode get() = if (payload.existingPosition) StakingMode.ADD_STAKE else StakingMode.NEW_POSITION

    private fun executeStake(request: BittensorRootStakeRequest, fee: Fee) = launchUnit {
        if (!submission.value.canSubmit) return@launchUnit
        try {
            val details = stakingDetails.first()
            stakeBeforeSnapshot = bittensorRootStakeChange(details.positions, request.validator.hotkey, request.amountInPlanks).before
            submission.value = BittensorSubmissionState.Submitting(mapProgress(details, request))
            operationAnalytics.startOperation()
            flowContinued = true
            analytics.track(
                BittensorAnalyticsEvent.Confirmed(
                    BittensorAnalyticsEvent.NETWORK,
                    StakingAnalyticsTarget.ROOT,
                    analyticsMode,
                    false,
                    null
                )
            )
            val result = bittensorStakingInteractor.executeRootStake(request)
            operationAnalytics.outcome(result, StakingAnalyticsTarget.ROOT, StakingFlow.STAKE, mode = analyticsMode)
            when (result) {
                is BittensorOperationResult.Success -> {
                    submission.value = BittensorSubmissionState.Completed(mapSuccess(details, request, fee, result.outcome))
                }
                is BittensorOperationResult.Failure -> {
                    submission.value = result.toSubmissionState(resourceManager, resultNavigation.landingActions())
                }
                is BittensorOperationResult.Unknown ->
                    submission.value = result.mapResult(resourceManager, resultNavigation.landingActions()).toSubmissionState()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val result = e.mapBittensorSubmissionError()
            operationAnalytics.outcome(result, StakingAnalyticsTarget.ROOT, StakingFlow.STAKE, mode = analyticsMode)
            submission.value = result.mapResult(resourceManager, resultNavigation.landingActions()).toSubmissionState()
        } finally {
            validating.value = false
        }
    }

    /** A result sheet button; Try again hides the sheet so the stake can be confirmed again. */
    fun resultActionClicked(action: BittensorResultAction) {
        if (action !in submission.value.result?.actions.orEmpty()) return

        resultNavigation.activate(action) { submission.value = submission.value.dismissResult() }
    }

    private fun mapAmount(details: BittensorRootStakingDetails, request: BittensorRootStakeRequest): AmountModel {
        val amount = details.asset.token.amountFromPlanks(request.amountInPlanks)
        return AmountModel(
            token = formatToken(amount, details.asset.token.configuration.symbol),
            fiat = formatFiatOrNull(details.asset.token.toFiatOrNull(amount), details.asset.token.currency)
        )
    }

    private suspend fun mapValidator(request: BittensorRootStakeRequest): BittensorValidatorUi {
        return BittensorValidatorUi(
            request.validator.name,
            Icon.FromDrawable(
                iconGenerator.createAddressIcon(
                    request.validator.hotkey.value,
                    AddressIconGenerator.SIZE_MEDIUM,
                    AddressIconGenerator.BACKGROUND_TRANSPARENT
                )
            )
        )
    }

    private fun mapProgress(details: BittensorRootStakingDetails, request: BittensorRootStakeRequest): BittensorStakeResultUi.Progress {
        val amount = formatStakeAmount(details, request)
        val descriptionRes = if (payload.existingPosition) {
            R.string.bittensor_staking_more_progress_description
        } else {
            R.string.bittensor_staking_progress_description
        }
        return BittensorStakeResultUi.Progress(
            title = resourceManager.getString(R.string.bittensor_staking_progress_title),
            description = resourceManager.getString(descriptionRes, amount)
        )
    }

    private fun mapSuccess(
        details: BittensorRootStakingDetails,
        request: BittensorRootStakeRequest,
        fee: Fee,
        outcome: BittensorOperationOutcome?
    ): BittensorStakeResultUi.Success {
        val token = details.asset.token
        val symbol = token.configuration.symbol
        val staked = outcome?.tao ?: request.amountInPlanks
        val estimatePrefix = if (outcome?.tao == null) "≈ " else ""
        val amount = estimatePrefix + formatToken(token.amountFromPlanks(staked), symbol)
        val change = bittensorRootStakeChange(details.positions, request.validator.hotkey, staked)
        val before = stakeBeforeSnapshot ?: change.before
        return bittensorRootStakeSuccess(
            resources = resourceManager,
            existingPosition = payload.existingPosition,
            amount = amount,
            stakeAfter = estimatePrefix + formatToken(token.amountFromPlanks(before + staked), symbol),
            validatorName = request.validator.name,
            validatorCount = change.validatorCount,
            fee = outcome.formatNetworkFee(fee, request.chain, resourceManager, tokenFormatter),
            actions = resultNavigation.completedActions()
        )
    }

    private fun formatStakeAmount(details: BittensorRootStakingDetails, request: BittensorRootStakeRequest): String {
        val amount = details.asset.token.amountFromPlanks(request.amountInPlanks)
        return formatToken(amount, details.asset.token.configuration.symbol)
    }

    private fun formatToken(amount: BigDecimal, symbol: TokenSymbol): String =
        tokenFormatter.formatToken(amount, symbol).toString()

    private fun formatFiatOrNull(amount: BigDecimal?, currency: Currency): String? {
        if (amount == null) return null

        return fiatFormatter.formatFiat(amount, currency).toString()
    }
}
