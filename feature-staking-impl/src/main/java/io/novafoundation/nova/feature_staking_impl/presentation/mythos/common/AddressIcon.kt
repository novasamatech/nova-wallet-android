package io.novafoundation.nova.feature_staking_impl.presentation.mythos.common

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosCollator
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

suspend fun AddressIconGenerator.collatorAddressModel(collator: MythosCollator, chain: Chain) = createAccountAddressModel(
    chain = chain,
    address = chain.addressOf(collator.accountId.value),
    name = collator.identity?.display
)
