package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.mappers

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorSorting
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.rewardsToColoredText
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.rewardsToScoring
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.stakeToScoring
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.StakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.StakeTargetModel.ColoredText
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.extensions.fromHex

typealias CollatorModel = StakeTargetModel<Collator>

suspend fun mapCollatorToCollatorModel(
    collator: Collator,
    chain: Chain,
    addressIconGenerator: AddressIconGenerator,
    sorting: CollatorSorting = CollatorSorting.REWARDS,
    resourceManager: ResourceManager,
    token: Token,
): CollatorModel {
    val address = chain.addressOf(collator.accountIdHex.fromHex())

    val addressModel = addressIconGenerator.createAccountAddressModel(
        address = address,
        chain = chain,
        name = collator.identity?.display
    )

    val scoring = when (sorting) {
        CollatorSorting.REWARDS -> rewardsToScoring(collator.apr)
        CollatorSorting.MIN_STAKE -> stakeToScoring(collator.minimumStakeToGetRewards, token)
        CollatorSorting.TOTAL_STAKE -> stakeToScoring(collator.snapshot.total, token)
        CollatorSorting.OWN_STAKE -> stakeToScoring(collator.snapshot.bond, token)
    }

    val subtitle = when (sorting) {
        CollatorSorting.REWARDS -> {
            val formattedMinStake = mapAmountToAmountModel(collator.minimumStakeToGetRewards, token).token

            StakeTargetModel.Subtitle(
                label = resourceManager.getString(R.string.staking_min_stake).withSubtitleSLabelSuffix(),
                value = ColoredText(formattedMinStake, R.color.white),
            )
        }

        else -> collator.apr?.let { apr ->
            StakeTargetModel.Subtitle(
                label = resourceManager.getString(R.string.staking_rewards).withSubtitleSLabelSuffix(),
                value = rewardsToColoredText(apr)!!
            )
        }
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

private fun String.withSubtitleSLabelSuffix() = "$this:"
