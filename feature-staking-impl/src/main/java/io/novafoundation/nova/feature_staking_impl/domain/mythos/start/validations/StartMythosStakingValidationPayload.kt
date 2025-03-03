package io.novafoundation.nova.feature_staking_impl.domain.mythos.start.validations

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosCollator
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosDelegatorState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.stakeableBalance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigDecimal

class StartMythosStakingValidationPayload(
    val amount: BigDecimal,
    val fee: Fee,
    val collator: MythosCollator,
    val asset: Asset,
    val delegatorState: MythosDelegatorState,
    val currentBlockNumber: BlockNumber,
)

val StartMythosStakingValidationPayload.chainId: ChainId
    get() = asset.token.configuration.chainId

fun StartMythosStakingValidationPayload.stakeableAmount(): BigDecimal {
    val amountInPlanks = delegatorState.stakeableBalance(asset, currentBlockNumber)
    return asset.token.amountFromPlanks(amountInPlanks)
}
