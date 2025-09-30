package io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.validations

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.asDefault
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.mythos.claimRewards.validations.MythosClaimRewardsValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.mythos.redeem.validations.RedeemMythosStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.validations.StartMythosStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.mythos.unbond.validations.UnbondMythosStakingValidationFailure
import io.novafoundation.nova.feature_wallet_api.domain.validation.amountIsTooBig
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError
import io.novafoundation.nova.feature_wallet_api.domain.validation.zeroAmount
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import javax.inject.Inject

interface MythosStakingValidationFailureFormatter {

    fun formatStartStaking(failure: ValidationStatus.NotValid<StartMythosStakingValidationFailure>): TransformedFailure

    fun formatUnbond(failure: ValidationStatus.NotValid<UnbondMythosStakingValidationFailure>): TransformedFailure

    fun formatRedeem(reason: RedeemMythosStakingValidationFailure): TitleAndMessage

    fun formatClaimRewards(reason: MythosClaimRewardsValidationFailure): TitleAndMessage
}

@FeatureScope
class RealMythosStakingValidationFailureFormatter @Inject constructor(
    private val resourceManager: ResourceManager,
    private val amountFormatter: AmountFormatter
) : MythosStakingValidationFailureFormatter {

    override fun formatStartStaking(failure: ValidationStatus.NotValid<StartMythosStakingValidationFailure>): TransformedFailure {
        return when (val reason = failure.reason) {
            is StartMythosStakingValidationFailure.NotEnoughBalanceToPayFees -> handleNotEnoughFeeError(reason, resourceManager).asDefault()

            StartMythosStakingValidationFailure.NotEnoughStakeableBalance -> resourceManager.amountIsTooBig().asDefault()

            StartMythosStakingValidationFailure.NotPositiveAmount -> resourceManager.zeroAmount().asDefault()

            is StartMythosStakingValidationFailure.TooLowStakeAmount -> {
                val formattedMinStake = amountFormatter.formatAmountToAmountModel(reason.minimumStake, reason.asset).token

                val content = resourceManager.getString(R.string.common_amount_low) to
                    resourceManager.getString(R.string.staking_setup_amount_too_low, formattedMinStake)

                content.asDefault()
            }
        }
    }

    override fun formatUnbond(failure: ValidationStatus.NotValid<UnbondMythosStakingValidationFailure>): TransformedFailure {
        return when (val reason = failure.reason) {
            is UnbondMythosStakingValidationFailure.NotEnoughBalanceToPayFees -> handleNotEnoughFeeError(reason, resourceManager).asDefault()

            is UnbondMythosStakingValidationFailure.ReleaseRequestsLimitReached -> {
                val content = resourceManager.getString(R.string.staking_unbonding_limit_reached_title) to
                    resourceManager.getString(R.string.staking_unbonding_limit_reached_message, reason.limit)

                content.asDefault()
            }
        }
    }

    override fun formatRedeem(reason: RedeemMythosStakingValidationFailure): TitleAndMessage {
        return when (reason) {
            is RedeemMythosStakingValidationFailure.NotEnoughBalanceToPayFees -> handleNotEnoughFeeError(reason, resourceManager)
        }
    }

    override fun formatClaimRewards(reason: MythosClaimRewardsValidationFailure): TitleAndMessage {
        return when (reason) {
            MythosClaimRewardsValidationFailure.NonProfitableClaim -> resourceManager.getString(R.string.common_confirmation_title) to
                resourceManager.getString(R.string.staking_warning_tiny_payout)

            is MythosClaimRewardsValidationFailure.NotEnoughBalanceToPayFees -> handleNotEnoughFeeError(reason, resourceManager)
        }
    }
}
