package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.os.SystemClock
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.domain.isError
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.presentation.fallbackIcon
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.common.utils.images.asUrlIcon
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorEstimatesRepository
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BITTENSOR_ALPHA_DECIMALS
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimateContext
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPreparedSubnetStake
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorProtocolUnits
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakeDraft
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.bittensorMonthlyProjection
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFeeEstimate
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorStakeValidationFailureUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.forRequest
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorSubmissionState
import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.asFeeContextFromSelf
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefaultBy
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.view.swap.SwapAssetView
import io.novafoundation.nova.feature_wallet_api.presentation.view.swap.SwapAssetsView
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.expectedBlockTime
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class BittensorSubnetReviewViewModel(
    private val subnetId: BittensorNetuid.Subnet,
    private val totalInPlanks: BigInteger,
    private val metaId: Long,
    private val validatorAddress: String,
    private val bittensorSubnetInteractor: BittensorSubnetInteractor,
    private val router: StakingRouter,
    chainRegistry: ChainRegistry,
    walletUiUseCase: WalletUiUseCase,
    accountRepository: AccountRepository,
    private val bittensorSubnetStakingInteractor: BittensorSubnetStakingInteractor,
    private val resources: ResourceManager,
    private val icons: AssetIconProvider,
    private val externalActions: ExternalActions.Presentation,
    private val iconGenerator: AddressIconGenerator,
    private val retryController: BittensorRetryController,
    private val bittensorAmountFormatter: BittensorAmountFormatter,
    private val validationSystem: BittensorStakePreflightSystem,
    private val validationExecutor: ValidationExecutor,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    private val estimatesRepository: BittensorEstimatesRepository,
    private val tokenFormatter: TokenFormatter,
    private val chainStateRepository: ChainStateRepository
) : BaseViewModel(), ExternalActions by externalActions, Validatable by validationExecutor {

    val retryEvents = retryController.events

    private val chain by lazyAsync { chainRegistry.getChain(ChainGeneses.BITTENSOR) }

    val wallet = walletUiUseCase.walletUiFlow(metaId)
        .shareInBackground()

    val account = accountRepository.metaAccountFlow(metaId).map { walletUiUseCase.walletAddressModel(it, chain.await(), AddressIconGenerator.SIZE_MEDIUM) }
        .shareInBackground()

    private val submission = MutableStateFlow<BittensorSubmissionState>(BittensorSubmissionState.Ready)

    private fun unresolved(hash: String) = BittensorSubmissionState.Unresolved(
        hash,
        BittensorStakeResultUi.Failure(
            resources.getString(R.string.bittensor_submission_unknown_title),
            resources.getString(R.string.bittensor_submission_unknown_description),
            hash,
            canRetry = false
        )
    )

    val resultState = submission.map { state ->
        state.result.takeUnless { it is BittensorStakeResultUi.Success || it is BittensorStakeResultUi.Progress }
    }
        .shareInBackground()

    private val walletState = retryController.observe(
        step = "staking wallet",
        transform = { it.withSafeLoading() }
    ) {
        bittensorSubnetStakingInteractor.observeWallet(
            metaId
        )
    }
        .shareInBackground()

    private val walletContext = walletState.filterLoaded()
        .shareInBackground()

    private val preparationRefresh = MutableStateFlow(0)

    private val validating = MutableStateFlow(false)

    private val preparationState = retryController.observe(
        step = "stake preparation",
        transform = { it.withSafeLoading() }
    ) {
        preparationRefresh.flatMapLatest {
            flowOf {
                bittensorSubnetStakingInteractor.prepare(
                    metaId,
                    subnetId,
                    totalInPlanks,
                    validatorAddress
                )
            }
        }
    }
        .shareInBackground()

    private val draft = preparationState.map { it.dataOrNull?.draft }
        .shareInBackground()

    private val quote = draft.map { it?.operation?.quote }
        .shareInBackground()

    val feeCaption = quote.map { quote ->
        val serviceFeeRate = quote?.serviceFeeRate ?: return@map ""
        val percentage = serviceFeeRate.movePointRight(2).toStripTrailingZerosString()
        resources.getString(R.string.bittensor_subnet_fee_caption, percentage)
    }
        .shareInBackground()

    private val hasError = combine(
        walletState,
        preparationState
    ) { walletContext, preparation -> walletContext.isError() || preparation.isError() }
        .shareInBackground()

    private val yieldState = estimatesRepository.observeAlphaYields(
        walletContext.map { it.chain },
        subnetId
    )
        .shareInBackground()

    private val costBasisState = estimatesRepository.observeCostBasis(
        walletContext.map {
            it.takeIf { it.metaId == metaId }
                ?.let { BittensorEstimateContext(metaId, it.chain) }
        }
    )
        .shareInBackground()

    val monthlyEstimate = combine(
        yieldState,
        walletContext,
        quote,
        preparationState.map { it.dataOrNull?.subnet }
    ) { state, walletContext, quote, subnet ->
        val key = runCatching { AccountIdKey(walletContext.chain.accountIdOf(validatorAddress)) }
            .getOrNull()
        val annual = key?.let { state?.get(it) }
        if (quote == null || annual == null || subnet == null) {
            "—"
        } else {
            val alpha = bittensorMonthlyProjection(BigDecimal(quote.alphaOutInPlanks, BittensorProtocolUnits.ALPHA_PRECISION), annual)
            "≈ " + tokenFormatter.formatToken(
                alpha,
                TokenSymbol(subnet.symbol.ifBlank { "—" })
            )
        }
    }
        .shareInBackground()

    val projectedAveragePrice = combine(
        costBasisState,
        draft.map { it?.operation?.quote },
        walletContext
    ) { state, quote, walletContext ->
        state.map { basis ->
            val current = basis?.forSubnet(subnetId.value)
            if (current == null || quote == null) {
                "—"
            } else {
                val projected = current.adding(quote.totalTaoInPlanks, quote.alphaOutInPlanks)
                "≈ " + tokenFormatter.formatToken(projected.averagePrice, walletContext.asset.token.configuration.symbol)
            }
        }
    }
        .shareInBackground()

    private val feeContext = walletContext
        .map { it.asset.token.configuration }
        .asFeeContextFromSelf()

    val feeLoaderMixin = feeLoaderMixinFactory.createDefaultBy<BittensorFeeEstimate<BittensorSubnetStakeDraft>>(
        scope = this,
        feeContext = feeContext
    )

    val fee = combine(
        draft,
        feeLoaderMixin.fee
    ) { draft, status ->
        status.forRequest(draft)
    }
        .shareInBackground()

    val validator = draft.map { it?.validator?.name ?: resources.getString(R.string.bittensor_select_validator) }
        .shareInBackground()

    private val nativeToken = walletContext.map { it.asset.token }
        .shareInBackground()

    val assets = combine(
        nativeToken,
        preparationState
    ) { token, preparation -> mapAssets(token, preparation.dataOrNull) }
        .shareInBackground()

    private var executionStartedAt = 0L

    private var executionCompletedAt = 0L

    private val duration = flowOf { chainStateRepository.expectedBlockTime(ChainGeneses.BITTENSOR) }
        .catch { emit(Duration.ZERO) }
        .shareInBackground()

    val isExecuting = submission.map { it is BittensorSubmissionState.Submitting }
        .shareInBackground()

    val isCompleted = submission.map { it is BittensorSubmissionState.Completed }
        .shareInBackground()

    val showExecution = submission.map { it is BittensorSubmissionState.Submitting || it is BittensorSubmissionState.Completed }
        .shareInBackground()

    fun remainingExecutionTime(): Duration = (
        (duration.replayCache.lastOrNull() ?: Duration.ZERO) -
            (SystemClock.elapsedRealtime() - executionStartedAt).milliseconds
        )
        .coerceAtLeast(Duration.ZERO)

    val executionSubtitle = combine(
        preparationState,
        submission
    ) { prepared, state ->
        if (state is BittensorSubmissionState.Completed) {
            resources.formatDateTime(executionCompletedAt)
        } else {
            resources.getString(
                R.string.bittensor_subnet_execution_buying,
                prepared.dataOrNull?.subnet?.name.orEmpty(),
                prepared.dataOrNull?.subnet?.symbol.orEmpty()
            )
        }
    }
        .shareInBackground()

    val executionAssets = assets.map { assets ->
        val output = assets.assetOut
        SwapAssetsView.Model(
            assetIn = assets.assetIn,
            assetOut = SwapAssetView.Model(
                assetIcon = output.assetIcon,
                amount = output.amount.copy(token = "≈ \u200e${output.amount.token}"),
                iconSizeDp = output.iconSizeDp,
                clipIconToOutline = output.clipIconToOutline
            )
        )
    }
        .shareInBackground()

    val rate = combine(
        nativeToken,
        preparationState
    ) { token, preparation -> formatRate(token, preparation.dataOrNull) }
        .shareInBackground()

    val slippage = bittensorSubnetInteractor.slippage.fraction.multiply(BigDecimal(100)).toStripTrailingZerosString() + "%"

    val validatorIcon = draft.map { prepared ->
        prepared?.validator?.let {
            iconGenerator.createAddressIcon(it.hotkey.value, AddressIconGenerator.SIZE_MEDIUM, AddressIconGenerator.BACKGROUND_TRANSPARENT)
        }
    }
        .shareInBackground()

    val confirmButtonState = combine(
        draft,
        validating,
        submission,
        hasError,
        fee
    ) { draft, validating, submission, failed, fee ->
        val label = resources.getString(R.string.common_confirm)
        val canConfirm = draft != null && !validating && submission.canSubmit && !failed && fee.loadedFeeOrNull()
            ?.request == draft
        if (canConfirm) DescriptiveButtonState.Enabled(label) else DescriptiveButtonState.Disabled(label)
    }
        .shareInBackground()

    init {
        feeLoaderMixin.connectWith(draft) { _, draft ->
            draft?.let {
                val fee = bittensorSubnetStakingInteractor.estimateFee(it)
                BittensorFeeEstimate(it, fee)
            }
        }
    }

    fun accountClicked() = launchUnit {
        externalActions.showAddressActions(
            account.first().address,
            chain.await()
        )
    }

    fun confirmClicked() = launchUnit {
        if (!submission.value.canSubmit || hasError.first() || validating.value) return@launchUnit
        val prepared = draft.first() ?: return@launchUnit
        val fee = fee.first()
            .loadedFeeOrNull()
            ?.takeIf { it.request == prepared } ?: return@launchUnit
        validating.value = true
        try {
            val payload = bittensorSubnetStakingInteractor.validationPayload(prepared, fee.fee)
            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                progressConsumer = validating.progressConsumer(),
                validationFailureTransformerCustom = { failure, actions ->
                    bittensorStakeValidationFailureUi(
                        failure,
                        actions,
                        resources
                    ) { preparationRefresh.value += 1 }
                }
            ) { executeStake(prepared, fee.fee) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showError(e)
        } finally {
            validating.value = false
        }
    }

    private fun executeStake(prepared: BittensorSubnetStakeDraft, fee: Fee) = launchUnit {
        if (!submission.value.canSubmit) return@launchUnit
        executionStartedAt = SystemClock.elapsedRealtime()
        submission.value = BittensorSubmissionState.Submitting(mapProgress())
        try {
            when (val result = bittensorSubnetStakingInteractor.execute(prepared)) {
                is BittensorOperationResult.Success -> {
                    executionCompletedAt = System.currentTimeMillis()
                    submission.value = BittensorSubmissionState.Completed(mapSuccess(prepared, fee))
                }
                is BittensorOperationResult.Failure -> {
                    showFailure(result.cause)
                }
                is BittensorOperationResult.Unknown -> submission.value = unresolved(result.transactionHash)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showFailure(e)
        } finally {
            validating.value = false
        }
    }

    private fun mapProgress(): BittensorStakeResultUi.Progress = BittensorStakeResultUi.Progress(
        resources.getString(R.string.bittensor_staking_progress_title),
        resources.getString(R.string.bittensor_subnet_staking_progress, subnetId.value)
    )

    private suspend fun mapSuccess(draft: BittensorSubnetStakeDraft, fee: Fee): BittensorStakeResultUi.Success {
        val token = walletContext.first().asset.token
        return BittensorStakeResultUi.Success(
            resources.getString(R.string.bittensor_subnet_staking_success, subnetId.value),
            resources.getString(R.string.bittensor_subnet_staking_completed),
            draft.validator.name,
            bittensorAmountFormatter.fee(fee, token)
        )
    }

    private fun showFailure(cause: Throwable) {
        submission.value = BittensorSubmissionState.Failed(
            BittensorStakeResultUi.Failure(
                resources.getString(R.string.bittensor_staking_failed_title),
                resources.getString(R.string.bittensor_staking_failed_description),
                cause.message ?: resources.getString(R.string.common_error_general_title)
            )
        )
    }

    private fun mapAssets(token: TokenBase, prepared: BittensorPreparedSubnetStake?): SwapAssetsView.Model = SwapAssetsView.Model(
        assetIn = mapInputAsset(token),
        assetOut = mapOutputAsset(token, prepared)
    )

    private fun mapInputAsset(token: TokenBase): SwapAssetView.Model {
        val input = token.amountFromPlanks(totalInPlanks)
        return SwapAssetView.Model(
            assetIcon = icons.getAssetIconOrFallback(token.configuration),
            amount = bittensorAmountFormatter.amount(
                input,
                token.configuration.symbol,
                token.toFiatOrNull(input),
                token.currency
            ),
            iconSizeDp = 48
        )
    }

    private fun mapOutputAsset(token: TokenBase, prepared: BittensorPreparedSubnetStake?): SwapAssetView.Model = SwapAssetView.Model(
        assetIcon = prepared?.subnet?.logoUrl?.asUrlIcon() ?: AssetIconProvider.fallbackIcon,
        amount = mapOutputAmount(token, prepared),
        iconSizeDp = 48,
        clipIconToOutline = true
    )

    private fun mapOutputAmount(token: TokenBase, prepared: BittensorPreparedSubnetStake?): AmountModel {
        if (prepared == null) return AmountModel("—", null)

        val output = BigDecimal(prepared.draft.operation.quote.alphaOutInPlanks, BITTENSOR_ALPHA_DECIMALS)
        val alphaPrice = prepared.subnet.priceInRao?.let(token::amountFromPlanks)
        val outputFiat = alphaPrice?.let { token.toFiatOrNull(output * it) }
        val symbol = TokenSymbol(prepared.subnet.symbol.ifBlank { "—" })
        return bittensorAmountFormatter.amount(
            output,
            symbol,
            outputFiat,
            token.currency
        )
    }

    private fun formatRate(token: TokenBase, prepared: BittensorPreparedSubnetStake?): String {
        if (prepared == null) return "—"
        return bittensorAmountFormatter.exchangeRate(prepared.draft.operation.quote, prepared.subnet, token)
    }

    fun resultDismissed() {
        submission.value = submission.value.dismissResult()
    }

    fun viewPositionClicked() {
        if (submission.value is BittensorSubmissionState.Completed) {
            router.openBittensorPortfolio()
        }
    }

    fun back() {
        if (submission.value !is BittensorSubmissionState.Submitting) router.back()
    }
}
