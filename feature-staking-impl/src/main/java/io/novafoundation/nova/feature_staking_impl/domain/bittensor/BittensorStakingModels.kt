package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
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
    val dataStatus: BittensorDataStatus,
    val position: BittensorRootPosition? = null,
    val positions: List<BittensorRootPosition> = listOfNotNull(position)
)

data class BittensorValidator(
    val hotkey: AccountIdKey,
    val name: String
)

data class BittensorRootPosition(
    val validatorHotkey: AccountIdKey,
    val stakeInPlanks: BigInteger
)

data class BittensorRootStakeRequest(
    val metaId: Long,
    val chain: Chain,
    val validator: BittensorValidator,
    val amountInPlanks: BigInteger
)

sealed interface BittensorDataStatus {

    data object Loading : BittensorDataStatus

    data object Fresh : BittensorDataStatus

    data class Stale(val cause: Throwable) : BittensorDataStatus
}

sealed interface BittensorOperationResult {

    data class Success(val transactionHash: String) : BittensorOperationResult

    data class Failure(val cause: Throwable, val transactionHash: String? = null) : BittensorOperationResult

    data class Unknown(val transactionHash: String) : BittensorOperationResult
}

internal fun requireBittensorImmediateWallet(wallet: io.novafoundation.nova.feature_account_api.domain.model.MetaAccount) {
    require(wallet.type != io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.MULTISIG) {
        "Bittensor multisig approval flows are not supported yet"
    }
}
