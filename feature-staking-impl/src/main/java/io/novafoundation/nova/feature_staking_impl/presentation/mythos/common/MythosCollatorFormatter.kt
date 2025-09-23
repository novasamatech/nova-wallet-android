package io.novafoundation.nova.feature_staking_impl.presentation.mythos.common

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.toHex
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.takeUnlessZero
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosCollator
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.recommendations.MythosCollatorRecommendationConfig
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.recommendations.MythosCollatorSorting
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.rewardsToColoredText
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.rewardsToScoring
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.stakeToScoring
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.model.MythosCollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.model.MythosCollatorWithAmount
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.model.MythosSelectCollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.mappers.withSubtitleLabelSuffix
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.selectCollators.labeledAmountSubtitle
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.StakeTargetModel
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.chain
import javax.inject.Inject

interface MythosCollatorFormatter {

    suspend fun collatorToSelectUi(
        collator: MythosCollatorWithAmount,
        token: Token
    ): MythosSelectCollatorModel

    suspend fun collatorToUi(
        collator: MythosCollator,
        token: Token,
        recommendationConfig: MythosCollatorRecommendationConfig
    ): MythosCollatorModel
}

@FeatureScope
class RealMythosCollatorFormatter @Inject constructor(
    private val stakingSharedState: StakingSharedState,
    private val resourceManager: ResourceManager,
    private val addressIconGenerator: AddressIconGenerator,
    private val amountFormatter: AmountFormatter
) : MythosCollatorFormatter {

    override suspend fun collatorToSelectUi(
        collatorWithAmount: MythosCollatorWithAmount,
        token: Token
    ): MythosSelectCollatorModel {
        return mapCollatorToSelectCollatorModel(
            collator = collatorWithAmount.target,
            stakedAmount = collatorWithAmount.stake.takeUnlessZero(),
            token = token
        )
    }

    override suspend fun collatorToUi(
        collator: MythosCollator,
        token: Token,
        recommendationConfig: MythosCollatorRecommendationConfig
    ): MythosCollatorModel {
        return mapCollatorToCollatorModel(
            collator = collator,
            chain = stakingSharedState.chain(),
            sorting = recommendationConfig.sorting,
            token = token
        )
    }

    private suspend fun mapCollatorToSelectCollatorModel(
        collator: MythosCollator,
        token: Token,
        stakedAmount: Balance? = null,
        active: Boolean = true,
    ): MythosSelectCollatorModel {
        val addressModel = addressIconGenerator.collatorAddressModel(collator, stakingSharedState.chain())
        val stakedAmountModel = stakedAmount?.let { amountFormatter.formatAmountToAmountModel(stakedAmount, token) }

        val subtitle = stakedAmountModel?.let {
            resourceManager.labeledAmountSubtitle(R.string.staking_main_stake_balance_staked, it, selectionActive = active)
        }

        return MythosSelectCollatorModel(
            addressModel = addressModel,
            payload = collator,
            active = active,
            subtitle = subtitle
        )
    }

    private suspend fun mapCollatorToCollatorModel(
        collator: MythosCollator,
        chain: Chain,
        sorting: MythosCollatorSorting,
        token: Token,
    ): MythosCollatorModel {
        val addressModel = addressIconGenerator.collatorAddressModel(collator, chain)

        val scoring = when (sorting) {
            MythosCollatorSorting.REWARDS -> rewardsToScoring(collator.apr)
            MythosCollatorSorting.TOTAL_STAKE -> stakeToScoring(collator.totalStake, token)
        }

        val subtitle = when (sorting) {
            MythosCollatorSorting.REWARDS -> null

            MythosCollatorSorting.TOTAL_STAKE -> StakeTargetModel.Subtitle(
                label = resourceManager.getString(R.string.staking_rewards).withSubtitleLabelSuffix(),
                value = rewardsToColoredText(collator.apr)!!
            )
        }

        return MythosCollatorModel(
            accountIdHex = collator.accountId.toHex(),
            slashed = false,
            addressModel = addressModel,
            stakeTarget = collator,
            isChecked = null,
            scoring = scoring,
            subtitle = subtitle
        )
    }
}
