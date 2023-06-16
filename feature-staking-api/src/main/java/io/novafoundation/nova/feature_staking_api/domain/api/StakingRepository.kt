package io.novafoundation.nova.feature_staking_api.domain.api

import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.RewardDestination
import io.novafoundation.nova.feature_staking_api.domain.model.SlashingSpans
import io.novafoundation.nova.feature_staking_api.domain.model.StakingLedger
import io.novafoundation.nova.feature_staking_api.domain.model.StakingStory
import io.novafoundation.nova.feature_staking_api.domain.model.ValidatorPrefs
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

typealias ExposuresWithEraIndex = Pair<AccountIdMap<Exposure>, EraIndex>

interface StakingRepository {

    suspend fun eraStartSessionIndex(chainId: ChainId, currentEra: BigInteger): EraIndex

    suspend fun eraLength(chainId: ChainId): BigInteger

    suspend fun getActiveEraIndex(chainId: ChainId): EraIndex

    suspend fun getCurrentEraIndex(chainId: ChainId): EraIndex

    suspend fun getHistoryDepth(chainId: ChainId): BigInteger

    fun observeActiveEraIndex(chainId: ChainId): Flow<EraIndex>

    suspend fun getElectedValidatorsExposure(chainId: ChainId, eraIndex: EraIndex): AccountIdMap<Exposure>

    suspend fun getValidatorPrefs(chainId: ChainId, accountIdsHex: List<String>): AccountIdMap<ValidatorPrefs?>

    suspend fun getSlashes(chainId: ChainId, accountIdsHex: List<String>): AccountIdMap<Boolean>

    suspend fun getSlashingSpan(chainId: ChainId, accountId: AccountId): SlashingSpans?

    fun stakingStateFlow(
        chain: Chain,
        chainAsset: Chain.Asset,
        accountId: AccountId
    ): Flow<StakingState>

    fun stakingStoriesFlow(): Flow<List<StakingStory>>

    suspend fun ledgerFlow(stakingState: StakingState.Stash): Flow<StakingLedger>

    suspend fun ledger(chainId: ChainId, accountId: AccountId): StakingLedger?

    suspend fun getRewardDestination(stakingState: StakingState.Stash): RewardDestination

    suspend fun minimumNominatorBond(chainId: ChainId): BigInteger

    suspend fun maxNominators(chainId: ChainId): BigInteger?

    suspend fun nominatorsCount(chainId: ChainId): BigInteger?

    fun electedExposuresInActiveEra(chainId: ChainId): Flow<ExposuresWithEraIndex>
}

suspend fun StakingRepository.historicalEras(chainId: ChainId): List<BigInteger> {
    val activeEra = getActiveEraIndex(chainId).toInt()
    val currentEra = getCurrentEraIndex(chainId).toInt()
    val historyDepth = getHistoryDepth(chainId).toInt()

    val startingIndex = (currentEra - historyDepth).coerceAtLeast(0)
    val historicalRange = startingIndex until activeEra

    return historicalRange.map(Int::toBigInteger)
}
