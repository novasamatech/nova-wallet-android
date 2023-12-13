package io.novafoundation.nova.feature_staking_impl.presentation.payouts.list

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.model.PendingPayout
import io.novafoundation.nova.feature_staking_impl.domain.model.PendingPayoutsStatistics
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.confirm.model.ConfirmPayoutPayload
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.list.model.PendingPayoutModel
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.list.model.PendingPayoutsStatisticsModel
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.model.PendingPayoutParcelable
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenChange
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PayoutsListViewModel(
    private val router: StakingRouter,
    private val resourceManager: ResourceManager,
    private val interactor: StakingInteractor,
) : BaseViewModel(), Retriable {

    override val retryEvent: MutableLiveData<Event<RetryPayload>> = MutableLiveData()

    private val payoutsStatisticsFlow = singleReplaySharedFlow<PendingPayoutsStatistics>()

    val payoutsStatisticsState = payoutsStatisticsFlow
        .map(::convertToUiModel)
        .withLoading()
        .inBackground()

    init {
        loadPayouts()
    }

    fun backClicked() {
        router.back()
    }

    fun payoutAllClicked() {
        launch {
            val payoutStatistics = payoutsStatisticsFlow.first()

            val payload = ConfirmPayoutPayload(
                totalRewardInPlanks = payoutStatistics.totalAmountInPlanks,
                payouts = payoutStatistics.payouts.map { mapPayoutToParcelable(it) }
            )

            router.openConfirmPayout(payload)
        }
    }

    fun payoutClicked(index: Int) {
        launch {
            val payouts = payoutsStatisticsFlow.first().payouts
            val payout = payouts[index]

            val payoutParcelable = mapPayoutToParcelable(payout)

            router.openPayoutDetails(payoutParcelable)
        }
    }

    private fun loadPayouts() {
        launch {
            val result = interactor.calculatePendingPayouts(viewModelScope)

            result.onSuccess { value ->
                payoutsStatisticsFlow.emit(value)
            }.onFailure { exception ->
                val errorMessage = exception.message ?: resourceManager.getString(R.string.common_undefined_error_message)

                Log.w("RX", "Failed to construct payout list", exception)

                retryEvent.value = Event(
                    RetryPayload(
                        title = resourceManager.getString(R.string.common_error_general_title),
                        message = errorMessage,
                        onRetry = ::loadPayouts,
                        onCancel = ::backClicked
                    )
                )
            }
        }
    }

    private suspend fun convertToUiModel(
        statistics: PendingPayoutsStatistics,
    ): PendingPayoutsStatisticsModel {
        val token = interactor.currentAssetFlow().first().token
        val totalAmount = token.amountFromPlanks(statistics.totalAmountInPlanks).formatTokenAmount(token.configuration)

        val payouts = statistics.payouts.map { mapPayoutToPayoutModel(token, it) }

        return PendingPayoutsStatisticsModel(
            payouts = payouts,
            payoutAllTitle = resourceManager.getString(R.string.staking_reward_payouts_payout_all, totalAmount),
            placeholderVisible = payouts.isEmpty()
        )
    }

    private fun mapPayoutToPayoutModel(token: Token, payout: PendingPayout): PendingPayoutModel {
        return with(payout) {
            val amount = token.amountFromPlanks(amountInPlanks)

            PendingPayoutModel(
                validatorTitle = validatorInfo.identityName ?: validatorInfo.address,
                timeLeft = timeLeft,
                createdAt = timeLeftCalculatedAt,
                daysLeftColor = if (closeToExpire) R.color.text_negative else R.color.text_secondary,
                amount = amount.formatTokenChange(token.configuration, isIncome = true),
                amountFiat = token.amountToFiat(amount).formatAsCurrency(token.currency)
            )
        }
    }

    private fun mapPayoutToParcelable(payout: PendingPayout): PendingPayoutParcelable {
        return with(payout) {
            PendingPayoutParcelable(
                validatorInfo = PendingPayoutParcelable.ValidatorInfoParcelable(
                    address = validatorInfo.address,
                    identityName = validatorInfo.identityName
                ),
                era = era,
                amountInPlanks = amountInPlanks,
                timeLeftCalculatedAt = timeLeftCalculatedAt,
                timeLeft = timeLeft,
                closeToExpire = closeToExpire,
                pagesToClaim = pagesToClaim
            )
        }
    }
}
