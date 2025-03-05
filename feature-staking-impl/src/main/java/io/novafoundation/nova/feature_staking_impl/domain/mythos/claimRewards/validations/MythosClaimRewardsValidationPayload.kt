package io.novafoundation.nova.feature_staking_impl.domain.mythos.claimRewards.validations

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import java.math.BigDecimal

class MythosClaimRewardsValidationPayload(
    val fee: Fee,
    val pendingRewardsPlanks: Balance,
    val asset: Asset,
)

val MythosClaimRewardsValidationPayload.pendingRewards: BigDecimal
    get() = asset.token.amountFromPlanks(pendingRewardsPlanks)
