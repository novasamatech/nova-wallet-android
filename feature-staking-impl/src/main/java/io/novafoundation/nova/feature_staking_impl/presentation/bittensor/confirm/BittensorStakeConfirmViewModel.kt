package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
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
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicRecoveryState
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicSubmissionRecovery
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakeRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakingDetails
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFeeEstimate
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.forRequest
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.message
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorSubmissionState
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.toUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorFeeUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorValidatorUi
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
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

class BittensorStakeConfirmViewModel(
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
    private val recovery: ExtrinsicSubmissionRecovery
) : BaseViewModel(), ExternalActions by externalActions, Validatable by validationExecutor {

    private val chain by lazyAsync { chainRegistry.getChain(ChainGeneses.BITTENSOR) }

    val retryEvents = retryController.events

    private val stakeRequest = flowOf { payload.toStakeRequest(chain.await()) }
        .shareInBackground()

    private val stakingDetailsState = retryController.observe(
        step = "root confirmation",
        transform = { it.withSafeLoading() }
    ) {
        bittensorStakingInteractor.observeRootStakingDetails(payload.metaId)
    }
        .shareInBackground()

    private val stakingDetails = combine(
        stakingDetailsState.filterLoaded(),
        stakeRequest
    ) { details, request ->
        bittensorStakingInteractor.detailsForValidator(details, request.validator)
    }
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

    val stakeAfter = combine(
        stakingDetails,
        stakeRequest
    ) { details, request ->
        val before = details.position?.stakeInPlanks ?: BigInteger.ZERO
        val token = details.asset.token
        formatToken(token.amountFromPlanks(before), token.configuration.symbol) + " → " +
            formatToken(token.amountFromPlanks(before + request.amountInPlanks), token.configuration.symbol)
    }
        .shareInBackground()

    val stakingType = resourceManager.getString(R.string.bittensor_root_staking)

    val validator = stakeRequest.map(::mapValidator)
        .shareInBackground()

    val apy = stakingDetails.map { bittensorAmountFormatter.apy(it.annualPercentageYield, it.asset.token.configuration.symbol) }
        .shareInBackground()

    private val submission = MutableStateFlow<BittensorSubmissionState>(BittensorSubmissionState.Ready)

    private var submittedHash: String? = null

    private val recoveryState = recovery.observe(payload.metaId, ChainGeneses.BITTENSOR).onEach { state ->
        if (submission.value !is BittensorSubmissionState.Submitting) {
            when (state) {
                is ExtrinsicRecoveryState.Pending -> submission.value = unresolved(state.hash)
                is ExtrinsicRecoveryState.Resolved -> if (submission.value is BittensorSubmissionState.Unresolved) {
                    submission.value = BittensorSubmissionState.Recovered(state.result.toUi(resourceManager, payload.metaId))
                }
                ExtrinsicRecoveryState.None -> Unit
            }
        }
    }
        .shareInBackground()

    private fun unresolved(hash: String) = BittensorSubmissionState.Unresolved(
        hash,
        BittensorStakeResultUi.Failure(
            resourceManager.getString(R.string.bittensor_submission_unknown_title),
            resourceManager.getString(R.string.bittensor_submission_unknown_description),
            hash,
            canRetry = false
        )
    )

    val resultState = submission.map { it.result }
        .shareInBackground()

    private val validating = MutableStateFlow(false)

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
        recoveryState.launchIn(this)
        feeLoaderMixin.connectWith(stakeRequest) { _, request ->
            val fee = bittensorStakingInteractor.estimateRootStakeFee(request)
            BittensorFeeEstimate(request, fee)
        }
    }

    fun back() {
        if (submission.value !is BittensorSubmissionState.Submitting) router.back()
    }

    fun accountClicked() = launchUnit {
        externalActions.showAddressActions(
            account.first().address,
            chain.await()
        )
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
            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                progressConsumer = validating.progressConsumer(),
                validationFailureTransformer = { resourceManager.getString(R.string.common_error_general_title) to it.message(resourceManager) }
            ) { executeStake(request, fee.fee) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showError(e)
        } finally {
            validating.value = false
        }
    }

    private fun executeStake(request: BittensorRootStakeRequest, fee: Fee) = launchUnit {
        if (!submission.value.canSubmit) return@launchUnit
        try {
            val details = stakingDetails.first()
            submission.value = BittensorSubmissionState.Submitting(mapProgress(details, request))
            when (val result = bittensorStakingInteractor.executeRootStake(request)) {
                is BittensorOperationResult.Success -> {
                    submittedHash = result.transactionHash
                    submission.value = BittensorSubmissionState.Completed(mapSuccess(details, request, fee))
                }
                is BittensorOperationResult.Failure -> {
                    submittedHash = result.transactionHash
                    submission.value = BittensorSubmissionState.Failed(mapFailure(result.cause))
                }
                is BittensorOperationResult.Unknown -> submission.value = unresolved(result.transactionHash)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            submission.value = BittensorSubmissionState.Failed(mapFailure(e))
        } finally {
            validating.value = false
        }
    }

    private fun acknowledgeResult() = launchUnit {
        val recovered = submission.value as? BittensorSubmissionState.Recovered
        val hash = recovered?.recovery?.transactionHash ?: submittedHash
        hash?.let { recovery.acknowledge(payload.metaId, it) }
    }

    fun resultDismissed() {
        acknowledgeResult()
        submission.value = submission.value.dismissResult()
    }

    fun viewPositionClicked() {
        acknowledgeResult()
        val recoverySuccessful = (submission.value as? BittensorSubmissionState.Recovered)?.recovery?.successful == true
        if (submission.value is BittensorSubmissionState.Completed || recoverySuccessful) {
            router.openBittensorRootDetails()
        }
    }

    private fun mapAmount(details: BittensorRootStakingDetails, request: BittensorRootStakeRequest): AmountModel {
        val amount = details.asset.token.amountFromPlanks(request.amountInPlanks)
        return AmountModel(
            token = formatToken(amount, details.asset.token.configuration.symbol),
            fiat = formatFiatOrDash(details.asset.token.toFiatOrNull(amount), details.asset.token.currency)
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
        return BittensorStakeResultUi.Progress(
            title = resourceManager.getString(R.string.bittensor_staking_progress_title),
            description = resourceManager.getString(R.string.bittensor_staking_progress_description, amount)
        )
    }

    private fun mapSuccess(details: BittensorRootStakingDetails, request: BittensorRootStakeRequest, fee: Fee): BittensorStakeResultUi.Success {
        val amount = formatStakeAmount(details, request)
        return BittensorStakeResultUi.Success(
            title = resourceManager.getString(R.string.bittensor_staking_success_title, amount),
            description = resourceManager.getString(R.string.bittensor_staking_success_description),
            validator = request.validator.name,
            fee = BittensorFeeUi(
                amount = formatToken(fee.decimalAmount, fee.asset.symbol),
                fiatAmount = formatFiatOrDash(details.asset.token.toFiatOrNull(fee.decimalAmount), details.asset.token.currency)
            )
        )
    }

    private fun mapFailure(cause: Throwable): BittensorStakeResultUi.Failure {
        return BittensorStakeResultUi.Failure(
            title = resourceManager.getString(R.string.bittensor_staking_failed_title),
            description = resourceManager.getString(R.string.bittensor_staking_failed_description),
            error = cause.message ?: resourceManager.getString(R.string.common_error_general_title)
        )
    }

    private fun formatStakeAmount(details: BittensorRootStakingDetails, request: BittensorRootStakeRequest): String {
        val amount = details.asset.token.amountFromPlanks(request.amountInPlanks)
        return formatToken(amount, details.asset.token.configuration.symbol)
    }

    private fun formatToken(amount: BigDecimal, symbol: TokenSymbol): String =
        tokenFormatter.formatToken(amount, symbol).toString()

    private fun formatFiatOrDash(amount: BigDecimal?, currency: Currency): String {
        if (amount == null) return "—"

        return fiatFormatter.formatFiat(amount, currency).toString()
    }
}
