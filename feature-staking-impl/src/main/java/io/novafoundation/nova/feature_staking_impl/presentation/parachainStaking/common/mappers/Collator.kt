package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.mappers

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.presentation.ColoredText
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegationState
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorSorting
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapIdentityToIdentityParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.rewardsToColoredText
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.rewardsToScoring
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.stakeToScoring
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.collators.collatorAddressModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.StakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.StakeTargetDetailsParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.StakeTargetStakeParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.StakeTargetStakeParcelModel.Active.UserStakeInfo
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.StakerParcelModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

typealias CollatorModel = StakeTargetModel<Collator>

suspend fun mapCollatorToCollatorModel(
    collator: Collator,
    chain: Chain,
    addressIconGenerator: AddressIconGenerator,
    sorting: CollatorSorting,
    resourceManager: ResourceManager,
    token: Token,
    amountFormatter: AmountFormatter
): CollatorModel {
    val addressModel = addressIconGenerator.collatorAddressModel(
        collator = collator,
        chain = chain
    )

    val scoring = when (sorting) {
        CollatorSorting.REWARDS -> rewardsToScoring(collator.apr)
        CollatorSorting.MIN_STAKE -> stakeToScoring(collator.minimumStakeToGetRewards, token)
        CollatorSorting.TOTAL_STAKE -> stakeToScoring(collator.snapshot?.total, token)
        CollatorSorting.OWN_STAKE -> stakeToScoring(collator.snapshot?.bond, token)
    }

    val subtitle = when (sorting) {
        CollatorSorting.REWARDS -> collator.minimumStakeToGetRewards?.let {
            val formattedMinStake = amountFormatter.formatAmountToAmountModel(it, token).token

            StakeTargetModel.Subtitle(
                label = resourceManager.getString(R.string.staking_min_stake).withSubtitleLabelSuffix(),
                value = ColoredText(formattedMinStake, R.color.text_primary),
            )
        }

        else -> StakeTargetModel.Subtitle(
            label = resourceManager.getString(R.string.staking_rewards).withSubtitleLabelSuffix(),
            value = rewardsToColoredText(collator.apr)!!
        )
    }

    return CollatorModel(
        accountIdHex = collator.accountIdHex,
        slashed = false,
        addressModel = addressModel,
        stakeTarget = collator,
        isChecked = null,
        scoring = scoring,
        subtitle = subtitle
    )
}

fun mapCollatorToDetailsParcelModel(
    collator: Collator,
    delegationState: DelegationState? = null
): StakeTargetDetailsParcelModel {
    val snapshot = collator.snapshot

    val stakeParcelModel = if (snapshot != null && collator.apr != null) {
        val isOversubscribed = delegationState == DelegationState.TOO_LOW_STAKE

        StakeTargetStakeParcelModel.Active(
            totalStake = snapshot.total,
            ownStake = snapshot.bond,
            stakers = snapshot.delegations.map {
                StakerParcelModel(
                    who = it.owner,
                    value = it.balance
                )
            },
            rewards = collator.apr,
            isOversubscribed = isOversubscribed,
            minimumStake = collator.minimumStakeToGetRewards,
            userStakeInfo = UserStakeInfo(willBeRewarded = !isOversubscribed),
        )
    } else {
        StakeTargetStakeParcelModel.Inactive
    }

    return StakeTargetDetailsParcelModel(
        accountIdHex = collator.accountIdHex,
        isSlashed = false,
        stake = stakeParcelModel,
        identity = collator.identity?.let(::mapIdentityToIdentityParcelModel)
    )
}

fun String.withSubtitleLabelSuffix() = "$this:"
