package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

data class BittensorRootStakingDetails(
    val metaId: Long,
    val chain: Chain,
    val asset: Asset,
    val validator: BittensorValidator?,
    val annualPercentageYield: BigDecimal?,
    val position: BittensorRootPosition?,
    val dataStatus: BittensorDataStatus
)

data class BittensorValidator(
    val hotkey: AccountIdKey,
    val name: String,
    val iconUrl: String?
)

data class BittensorRootPosition(
    val validatorHotkey: AccountIdKey,
    val stakeInPlanks: BigInteger
)

data class BittensorRootStakeDraft(
    val metaId: Long,
    val amountInPlanks: BigInteger,
    val validator: BittensorValidator,
    val fee: Fee
)

sealed interface BittensorDataStatus {

    data object Loading : BittensorDataStatus

    data object Fresh : BittensorDataStatus

    data class Stale(val cause: Throwable) : BittensorDataStatus
}
