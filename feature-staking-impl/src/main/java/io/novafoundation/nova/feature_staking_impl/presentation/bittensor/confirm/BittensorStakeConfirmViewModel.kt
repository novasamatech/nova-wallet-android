package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorRecoveryRequest
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorWalletRecovery
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakeDraft
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakingDetails
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakingInteractor
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicSubmissionRecovery
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicRecoveryState
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.toUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorFeeUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorValidatorUi
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import java.math.BigInteger
import java.math.BigDecimal
import java.text.NumberFormat

class BittensorStakeConfirmViewModel(
    private val resourceManager: ResourceManager,
    private val router: StakingRouter,
    private val externalActions: ExternalActions.Presentation,
    chainRegistry: ChainRegistry,
    walletUiUseCase: WalletUiUseCase,
    selectedAccountUseCase: SelectedAccountUseCase,
    private val interactor: BittensorStakingInteractor,
    private val iconGenerator: AddressIconGenerator,
    private val recovery: ExtrinsicSubmissionRecovery
) : BaseViewModel(), ExternalActions by externalActions {

    private val chain by lazyAsync { chainRegistry.getChain(ChainGeneses.BITTENSOR) }
    private val stakingDetails = interactor.observeRootStakingDetails()
        .catch { showError(it) }
        .shareInBackground()
    private val stakeDraft = interactor.observeStakeDraft().shareInBackground()

    val amount = combine(stakingDetails, stakeDraft, ::mapAmount).shareInBackground()
    val wallet = walletUiUseCase.selectedWalletUiFlow().shareInBackground()
    val account = selectedAccountUseCase.selectedAddressModelFlow(chain::await).shareInBackground()

    val fee = combine(stakingDetails, stakeDraft) { details, draft ->
        FeeStatus.Loaded(
            FeeModel(
                fee = Unit,
                display = FeeDisplay(
                    title = formatToken(draft.fee.decimalAmount, draft.fee.asset.symbol),
                    subtitle = formatFiatOrDash(details.asset.token.toFiatOrNull(draft.fee.decimalAmount), details)
                )
            )
        )
    }.shareInBackground()

    val stakeAfter = combine(stakingDetails, stakeDraft) { details, draft ->
        if (details.metaId != draft.metaId || details.validator.hotkey != draft.validator.hotkey ||
            details.dataStatus !is io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus.Fresh
        ) {
            return@combine "—"
        }
        val before = details.position?.stakeInPlanks ?: BigInteger.ZERO
        val token = details.asset.token
        formatToken(token.amountFromPlanks(before), token.configuration.symbol) + " → " +
            formatToken(token.amountFromPlanks(before + draft.amountInPlanks), token.configuration.symbol)
    }.shareInBackground()

    val stakingType = flowOf(resourceManager.getString(R.string.bittensor_root_staking)).shareInBackground()
    val validator = stakeDraft.map(::mapValidator).shareInBackground()
    val apy = stakingDetails.map(::formatApy).shareInBackground()
    private val _resultState = MutableStateFlow<BittensorStakeResultUi?>(null)
    val resultState = _resultState.asStateFlow()
    private val executing = MutableStateFlow(false)
    private val completed = MutableStateFlow(false)
    private data class ResultOrigin(val metaId: Long, val hash: String)

    private var resultOrigin: ResultOrigin? = null
    private var recoveringHash: String? = null

    private val recoveryState = stakingDetails.map { BittensorRecoveryRequest(it.metaId, it.chain.id) }.distinctUntilChanged()
        .flatMapLatest { (metaId, chainId) ->
            recovery.observe(metaId, chainId).map { BittensorWalletRecovery(metaId, it) }
                .onStart { emit(BittensorWalletRecovery(metaId, ExtrinsicRecoveryState.None)) }
        }
        .onEach { (recoveryMetaId, state) ->
            if (!executing.value) {
                when (state) {
                    is ExtrinsicRecoveryState.Pending -> {
                        recoveringHash = state.hash
                        _resultState.value = BittensorStakeResultUi.Failure(
                            resourceManager.getString(R.string.bittensor_submission_unknown_title),
                            resourceManager.getString(R.string.bittensor_submission_unknown_description),
                            state.hash,
                            canRetry = false
                        )
                    }
                    is ExtrinsicRecoveryState.Resolved -> {
                        val shownHash = (_resultState.value as? BittensorStakeResultUi.Failure)?.error
                        if (recoveringHash == state.result.hash || shownHash == state.result.hash) {
                            val recovered = state.result.toUi(resourceManager, recoveryMetaId)
                            completed.value = recovered.successful
                            _resultState.value = recovered
                            recoveringHash = null
                        }
                    }
                    ExtrinsicRecoveryState.None -> Unit
                }
            }
        }
        .onStart { emit(BittensorWalletRecovery(0L, ExtrinsicRecoveryState.None)) }
        .shareInBackground()

    val confirmButtonState = combine(stakingDetails, stakeDraft, executing, completed, recoveryState) { details, draft, executing, completed, recovery ->
        val label = resourceManager.getString(R.string.common_confirm)
        if (executing || completed || details.metaId != draft.metaId || details.validator.hotkey != draft.validator.hotkey ||
            recovery.state is ExtrinsicRecoveryState.Pending
        ) {
            DescriptiveButtonState.Disabled(label)
        } else {
            DescriptiveButtonState.Enabled(label)
        }
    }.onStart { emit(DescriptiveButtonState.Loading) }.shareInBackground()

    init {
        if (!interactor.hasDraft()) {
            launchUnit {
                showError(resourceManager.getString(R.string.bittensor_draft_expired))
                router.back()
            }
        }
    }

    fun back() {
        if (!executing.value) router.back()
    }

    fun accountClicked() = launchUnit {
        externalActions.showAddressActions(account.first().address, chain.await())
    }

    fun validatorClicked() = launchUnit {
        val draft = stakeDraft.first()
        externalActions.showAddressActions(chain.await().addressOf(draft.validator.hotkey.value), chain.await())
    }

    fun confirmClicked() = launchUnit {
        if (executing.value || completed.value) return@launchUnit
        executing.value = true
        try {
            val details = stakingDetails.first()
            val draft = stakeDraft.first()
            _resultState.value = mapProgress(details, draft)

            when (val result = interactor.executeRootStake()) {
                is BittensorOperationResult.Success -> {
                    resultOrigin = ResultOrigin(draft.metaId, result.transactionHash)
                    completed.value = true
                    _resultState.value = mapSuccess(details, draft)
                }
                is BittensorOperationResult.Failure -> {
                    resultOrigin = result.transactionHash?.let { ResultOrigin(draft.metaId, it) }
                    _resultState.value = mapFailure(result.cause)
                }
                is BittensorOperationResult.Unknown -> {
                    completed.value = true
                    _resultState.value = BittensorStakeResultUi.Failure(
                        resourceManager.getString(R.string.bittensor_submission_unknown_title),
                        resourceManager.getString(R.string.bittensor_submission_unknown_description),
                        result.transactionHash,
                        canRetry = false
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _resultState.value = mapFailure(e)
        } finally {
            executing.value = false
        }
    }

    fun retryClicked() {
        confirmClicked()
    }

    fun resultDismissed() {
        val recovered = _resultState.value as? BittensorStakeResultUi.Recovered
        _resultState.value = null
        val origin = recovered?.let { ResultOrigin(it.metaId, it.transactionHash) } ?: resultOrigin
        if (origin != null) launchUnit { recovery.acknowledge(origin.metaId, origin.hash) }
    }

    fun viewPositionClicked() = launchUnit {
        resultOrigin?.let { recovery.acknowledge(it.metaId, it.hash) }
        if (completed.value) router.openBittensorRootDetails()
    }

    private fun mapAmount(details: BittensorRootStakingDetails, draft: BittensorRootStakeDraft): AmountModel {
        val amount = details.asset.token.amountFromPlanks(draft.amountInPlanks)
        return AmountModel(
            token = formatToken(amount, details.asset.token.configuration.symbol),
            fiat = formatFiatOrDash(details.asset.token.toFiatOrNull(amount), details)
        )
    }

    private suspend fun mapValidator(draft: BittensorRootStakeDraft): BittensorValidatorUi {
        return BittensorValidatorUi(
            draft.validator.name,
            Icon.FromDrawable(
                iconGenerator.createAddressIcon(
                    draft.validator.hotkey.value,
                    AddressIconGenerator.SIZE_MEDIUM,
                    AddressIconGenerator.BACKGROUND_TRANSPARENT
                )
            )
        )
    }

    private fun formatApy(details: BittensorRootStakingDetails): String {
        val apy = details.annualPercentageYield?.toStripTrailingZerosString() ?: return "—"
        return resourceManager.getString(R.string.bittensor_apy_paid_in, "$apy%", details.asset.token.configuration.symbol)
    }

    private fun mapProgress(details: BittensorRootStakingDetails, draft: BittensorRootStakeDraft): BittensorStakeResultUi.Progress {
        val amount = formatDraftAmount(details, draft)
        return BittensorStakeResultUi.Progress(
            title = resourceManager.getString(R.string.bittensor_staking_progress_title),
            description = resourceManager.getString(R.string.bittensor_staking_progress_description, amount)
        )
    }

    private fun mapSuccess(details: BittensorRootStakingDetails, draft: BittensorRootStakeDraft): BittensorStakeResultUi.Success {
        val amount = formatDraftAmount(details, draft)
        return BittensorStakeResultUi.Success(
            title = resourceManager.getString(R.string.bittensor_staking_success_title, amount),
            description = resourceManager.getString(R.string.bittensor_staking_success_description),
            validator = draft.validator.name,
            fee = BittensorFeeUi(
                amount = formatToken(draft.fee.decimalAmount, draft.fee.asset.symbol),
                fiatAmount = formatFiatOrDash(details.asset.token.toFiatOrNull(draft.fee.decimalAmount), details)
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

    private fun formatDraftAmount(details: BittensorRootStakingDetails, draft: BittensorRootStakeDraft): String {
        val amount = details.asset.token.amountFromPlanks(draft.amountInPlanks)
        return formatToken(amount, details.asset.token.configuration.symbol)
    }

    private fun formatToken(amount: BigDecimal, symbol: TokenSymbol): String =
        "${amount.toStripTrailingZerosString()} ${symbol.value}"

    private fun formatFiatOrDash(amount: BigDecimal?, details: BittensorRootStakingDetails): String {
        if (amount == null) return "—"

        return NumberFormat.getCurrencyInstance().apply {
            currency = java.util.Currency.getInstance(details.asset.token.currency.code)
        }.format(amount)
    }
}
