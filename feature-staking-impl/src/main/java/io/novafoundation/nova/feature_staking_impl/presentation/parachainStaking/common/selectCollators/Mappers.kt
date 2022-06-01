package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.selectCollators

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.delegationAmountTo
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.SelectedCollator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.UnbondingCollator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.collators.collatorAddressModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.model.SelectCollatorModel
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.extensions.fromHex

suspend fun mapUnbondingCollatorToSelectCollatorModel(
    unbondingCollator: UnbondingCollator,
    chain: Chain,
    asset: Asset,
    addressIconGenerator: AddressIconGenerator,
) = mapSelectedCollatorToSelectCollatorModel(
    selectedCollator = unbondingCollator,
    active = unbondingCollator.hasPendingUnbonding.not(),
    chain = chain,
    asset = asset,
    addressIconGenerator = addressIconGenerator
)

suspend fun mapSelectedCollatorToSelectCollatorModel(
    selectedCollator: SelectedCollator,
    active: Boolean = true,
    chain: Chain,
    asset: Asset,
    addressIconGenerator: AddressIconGenerator,
) = mapCollatorToSelectCollatorModel(
    collator = selectedCollator.collator,
    stakedAmount = selectedCollator.delegation,
    chain = chain,
    active = active,
    asset = asset,
    addressIconGenerator = addressIconGenerator
)

suspend fun mapCollatorToSelectCollatorModel(
    collator: Collator,
    delegatorState: DelegatorState,
    asset: Asset,
    addressIconGenerator: AddressIconGenerator,
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
        addressIconGenerator = addressIconGenerator
    )
}

suspend fun mapCollatorToSelectCollatorModel(
    collator: Collator,
    stakedAmount: Balance? = null,
    active: Boolean = true,
    chain: Chain,
    asset: Asset,
    addressIconGenerator: AddressIconGenerator,
): SelectCollatorModel {
    val addressModel = addressIconGenerator.collatorAddressModel(collator, chain)
    val stakedAmountModel = stakedAmount?.let { mapAmountToAmountModel(stakedAmount, asset) }

    return SelectCollatorModel(
        addressModel = addressModel,
        amount = stakedAmountModel,
        payload = collator,
        active = active
    )
}
