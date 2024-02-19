package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.selectCollators

import androidx.annotation.StringRes
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.buildSpannable
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.delegationAmountTo
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.SelectedCollator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.UnbondingCollator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.collators.collatorAddressModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.model.SelectCollatorModel
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.extensions.fromHex

suspend fun mapUnbondingCollatorToSelectCollatorModel(
    unbondingCollator: UnbondingCollator,
    chain: Chain,
    asset: Asset,
    addressIconGenerator: AddressIconGenerator,
    resourceManager: ResourceManager,
): SelectCollatorModel = mapSelectedCollatorToSelectCollatorModel(
    selectedCollator = unbondingCollator,
    active = unbondingCollator.hasPendingUnbonding.not(),
    chain = chain,
    asset = asset,
    addressIconGenerator = addressIconGenerator,
    resourceManager = resourceManager,
)

suspend fun mapSelectedCollatorToSelectCollatorModel(
    selectedCollator: SelectedCollator,
    active: Boolean = true,
    chain: Chain,
    asset: Asset,
    addressIconGenerator: AddressIconGenerator,
    resourceManager: ResourceManager,
): SelectCollatorModel = mapCollatorToSelectCollatorModel(
    collator = selectedCollator.collator,
    stakedAmount = selectedCollator.delegation,
    chain = chain,
    active = active,
    asset = asset,
    addressIconGenerator = addressIconGenerator,
    resourceManager = resourceManager
)

suspend fun mapCollatorToSelectCollatorModel(
    collator: Collator,
    delegatorState: DelegatorState,
    asset: Asset,
    addressIconGenerator: AddressIconGenerator,
    resourceManager: ResourceManager,
    active: Boolean = true
): SelectCollatorModel {
    val chain = delegatorState.chain

    val collatorId = collator.accountIdHex.fromHex()
    val stakedAmount = delegatorState.castOrNull<DelegatorState.Delegator>()?.delegationAmountTo(collatorId)

    return mapCollatorToSelectCollatorModel(
        collator = collator,
        stakedAmount = stakedAmount,
        chain = chain,
        active = active,
        asset = asset,
        addressIconGenerator = addressIconGenerator,
        resourceManager = resourceManager,
    )
}

suspend fun mapCollatorToSelectCollatorModel(
    collator: Collator,
    stakedAmount: Balance? = null,
    active: Boolean = true,
    chain: Chain,
    asset: Asset,
    addressIconGenerator: AddressIconGenerator,
    resourceManager: ResourceManager,
): SelectCollatorModel {
    val addressModel = addressIconGenerator.collatorAddressModel(collator, chain)
    val stakedAmountModel = stakedAmount?.let { mapAmountToAmountModel(stakedAmount, asset) }

    val subtitle = stakedAmountModel?.let {
        resourceManager.labeledAmountSubtitle(R.string.staking_main_stake_balance_staked, it, selectionActive = active)
    }

    return SelectCollatorModel(
        addressModel = addressModel,
        payload = collator,
        active = active,
        subtitle = subtitle
    )
}

fun ResourceManager.labeledAmountSubtitle(
    @StringRes labelRes: Int,
    amount: AmountModel,
    selectionActive: Boolean
): CharSequence {
    val labelText = "${getString(labelRes)}: "

    return if (selectionActive) {
        buildSpannable(this) {
            appendColored(labelText, R.color.text_secondary)
            appendColored(amount.token, R.color.text_primary)
        }
    } else {
        buildSpannable(this) {
            appendColored(labelText + amount.token, R.color.text_secondary)
        }
    }
}
