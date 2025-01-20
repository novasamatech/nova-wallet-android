package io.novafoundation.nova.feature_staking_impl.presentation.mythos.common

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.takeUnlessZero
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosCollator
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.model.MythosCollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.model.MythosCollatorWithAmount
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.selectCollators.labeledAmountSubtitle
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.chain
import javax.inject.Inject

interface MythosCollatorFormatter {

    suspend fun collatorToUi(
        collator: MythosCollatorWithAmount,
        asset: Asset
    ): MythosCollatorModel
}

@FeatureScope
class RealMythosCollatorFormatter @Inject constructor(
    private val stakingSharedState: StakingSharedState,
    private val resourceManager: ResourceManager,
    private val addressIconGenerator: AddressIconGenerator,
) : MythosCollatorFormatter {

    override suspend fun collatorToUi(
        collatorWithAmount: MythosCollatorWithAmount,
        asset: Asset
    ): MythosCollatorModel {
        return mapCollatorToSelectCollatorModel(
            collator = collatorWithAmount.target,
            stakedAmount = collatorWithAmount.stake.takeUnlessZero(),
            asset = asset
        )
    }

    private suspend fun mapCollatorToSelectCollatorModel(
        collator: MythosCollator,
        asset: Asset,
        stakedAmount: Balance? = null,
        active: Boolean = true,
    ): MythosCollatorModel {
        val addressModel = addressIconGenerator.collatorAddressModel(collator, stakingSharedState.chain())
        val stakedAmountModel = stakedAmount?.let { mapAmountToAmountModel(stakedAmount, asset) }

        val subtitle = stakedAmountModel?.let {
            resourceManager.labeledAmountSubtitle(R.string.staking_main_stake_balance_staked, it, selectionActive = active)
        }

        return MythosCollatorModel(
            addressModel = addressModel,
            payload = collator,
            active = active,
            subtitle = subtitle
        )
    }
}
