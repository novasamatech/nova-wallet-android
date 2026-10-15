package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_account_api.data.model.decimalAmount
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakeDraft
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakingDetails
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorSubmissionState
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorFeeUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorValidatorUi
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class BittensorStakeConfirmViewModel(
    private val resourceManager: ResourceManager,
    private val router: StakingRouter,
    private val externalActions: ExternalActions.Presentation,
    chainRegistry: ChainRegistry,
    walletUiUseCase: WalletUiUseCase,
    selectedAccountUseCase: SelectedAccountUseCase,
    private val bittensorStakingInteractor: BittensorStakingInteractor,
    private val iconGenerator: AddressIconGenerator,
    private val fiatFormatter: FiatFormatter,
    private val tokenFormatter: TokenFormatter,
    private val bittensorAmountFormatter: BittensorAmountFormatter,
    private val retryController: BittensorRetryController
) : BaseViewModel(), ExternalActions by externalActions {

    private val chain by lazyAsync { chainRegistry.getChain(ChainGeneses.BITTENSOR) }

    val retryEvents = retryController.events

    private val stakingDetailsState = retryController.observe("root confirmation", bittensorStakingInteractor::observeRootStakingDetails).shareInBackground()

    private val stakingDetails = stakingDetailsState.filterLoaded().shareInBackground()

    private val stakeDraft = bittensorStakingInteractor.observeStakeDraft().filterNotNull().shareInBackground()

    val amount = combine(stakingDetails, stakeDraft, ::mapAmount).shareInBackground()

    val wallet = walletUiUseCase.selectedWalletUiFlow().shareInBackground()

    val account = selectedAccountUseCase.selectedAddressModelFlow(chain::await).shareInBackground()

    val fee = combine(stakingDetails, stakeDraft) { details, draft ->
        FeeStatus.Loaded(
            FeeModel(
                fee = Unit,
                display = FeeDisplay(
                    title = formatToken(draft.fee.decimalAmount, draft.fee.asset.symbol),
                    subtitle = formatFiatOrDash(details.asset.token.toFiatOrNull(draft.fee.decimalAmount), details.asset.token.currency)
                )
            )
        )
    }.shareInBackground()

    val stakingType = resourceManager.getString(R.string.bittensor_root_staking)

    val validator = stakeDraft.map(::mapValidator).shareInBackground()

    val apy = stakingDetails.map { bittensorAmountFormatter.apy(it.annualPercentageYield, it.asset.token.configuration.symbol) }.shareInBackground()

    private val submission = MutableStateFlow<BittensorSubmissionState>(BittensorSubmissionState.Ready)

    val resultState = submission.map { it.result }.shareInBackground()

    val confirmButtonState = combine(stakingDetailsState, stakeDraft, submission) { state, draft, submission ->
        val details = state.dataOrNull
        val label = resourceManager.getString(R.string.common_confirm)
        if (!submission.canSubmit || details == null || details.metaId != draft.metaId) {
            DescriptiveButtonState.Disabled(label)
        } else {
            DescriptiveButtonState.Enabled(label)
        }
    }.onStart { emit(DescriptiveButtonState.Loading) }.shareInBackground()

    init {
        if (!bittensorStakingInteractor.hasDraft()) {
            launchUnit {
                showError(resourceManager.getString(R.string.bittensor_draft_expired))
                router.back()
            }
        }
    }

    fun back() {
        if (submission.value !is BittensorSubmissionState.Submitting) router.back()
    }

    fun accountClicked() = launchUnit {
        externalActions.showAddressActions(account.first().address, chain.await())
    }

    fun confirmClicked() = launchUnit {
        if (!submission.value.canSubmit || stakingDetailsState.first().dataOrNull == null) return@launchUnit
        try {
            val details = stakingDetails.first()
            val draft = stakeDraft.first()
            if (!submission.value.canSubmit) return@launchUnit
            submission.value = BittensorSubmissionState.Submitting(mapProgress(details, draft))

            bittensorStakingInteractor.executeRootStake(chain.await(), draft)
                .onSuccess {
                    submission.value = BittensorSubmissionState.Completed(mapSuccess(details, draft))
                }
                .onFailure {
                    if (it is CancellationException) throw it
                    submission.value = BittensorSubmissionState.Failed(mapFailure(it))
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            submission.value = BittensorSubmissionState.Failed(mapFailure(e))
        }
    }

    fun resultDismissed() {
        submission.value = submission.value.dismissResult()
    }

    fun viewPositionClicked() {
        if (submission.value is BittensorSubmissionState.Completed) router.openBittensorRootDetails()
    }

    private fun mapAmount(details: BittensorRootStakingDetails, draft: BittensorRootStakeDraft): AmountModel {
        val amount = details.asset.token.amountFromPlanks(draft.amountInPlanks)
        return AmountModel(
            token = formatToken(amount, details.asset.token.configuration.symbol),
            fiat = formatFiatOrDash(details.asset.token.toFiatOrNull(amount), details.asset.token.currency)
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
                fiatAmount = formatFiatOrDash(details.asset.token.toFiatOrNull(draft.fee.decimalAmount), details.asset.token.currency)
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
        tokenFormatter.formatToken(amount, symbol).toString()

    private fun formatFiatOrDash(amount: BigDecimal?, currency: Currency): String {
        if (amount == null) return "—"

        return fiatFormatter.formatFiat(amount, currency).toString()
    }
}
