package io.novafoundation.nova.feature_staking_api.data.nominationPools.pool

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.domain.account.system.SystemAccountMatcher
import io.novafoundation.nova.feature_staking_api.domain.nominationPool.model.PoolId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId

interface PoolAccountDerivation {

    enum class PoolAccountType {

        BONDED, REWARD
    }

    suspend fun derivePoolAccount(poolId: PoolId, derivationType: PoolAccountType, chainId: ChainId): AccountId

    /**
     * Derives pool accounts with poolId from range 1..[numberOfPools] (end-inclusive)
     */
    suspend fun derivePoolAccountsRange(numberOfPools: Int, derivationType: PoolAccountType, chainId: ChainId): Map<PoolId, AccountIdKey>

    suspend fun poolAccountMatcher(derivationType: PoolAccountType, chainId: ChainId): SystemAccountMatcher?
}

suspend fun PoolAccountDerivation.poolRewardAccountMatcher(chainId: ChainId): SystemAccountMatcher? {
    return poolAccountMatcher(PoolAccountDerivation.PoolAccountType.REWARD, chainId)
}

suspend fun PoolAccountDerivation.bondedAccountOf(poolId: PoolId, chainId: ChainId): AccountId {
    return derivePoolAccount(poolId, PoolAccountDerivation.PoolAccountType.BONDED, chainId)
}

suspend fun PoolAccountDerivation.deriveAllBondedPools(lastPoolId: PoolId, chainId: ChainId): Map<PoolId, AccountIdKey> {
    return derivePoolAccountsRange(numberOfPools = lastPoolId.value.toInt(), PoolAccountDerivation.PoolAccountType.BONDED, chainId)
}
