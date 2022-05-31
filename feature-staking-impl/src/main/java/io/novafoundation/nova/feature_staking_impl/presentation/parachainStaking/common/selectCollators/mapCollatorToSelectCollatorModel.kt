package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.selectCollators

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.delegationAmountTo
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.model.SelectCollatorModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import jp.co.soramitsu.fearless_utils.extensions.fromHex

suspend fun mapCollatorToSelectCollatorModel(
    collator: Collator,
    delegatorState: DelegatorState,
    asset: Asset,
    addressIconGenerator: AddressIconGenerator
): SelectCollatorModel {
    val chain = delegatorState.chain

    val addressModel = addressIconGenerator.createAccountAddressModel(
        chain = chain,
        address = collator.address,
        name = collator.identity?.display
    )

    val collatorId = collator.accountIdHex.fromHex()
    val stakedAmount = delegatorState.castOrNull<DelegatorState.Delegator>()?.delegationAmountTo(collatorId)?.let {
        mapAmountToAmountModel(it, asset)
    }

    return SelectCollatorModel(
        addressModel = addressModel,
        staked = stakedAmount,
        collator = collator
    )
}
