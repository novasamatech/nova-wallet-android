package io.novafoundation.nova.feature_staking_impl.domain.mythos.unbond.validations

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosCollator
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosDelegatorState
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class UnbondMythosStakingValidationPayload(
    val fee: Fee,
    val collator: MythosCollator,
    val asset: Asset,
    val delegatorState: MythosDelegatorState,
)

val UnbondMythosStakingValidationPayload.chainId: ChainId
    get() = asset.token.configuration.chainId
