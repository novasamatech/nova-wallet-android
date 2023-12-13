package io.novafoundation.nova.feature_staking_impl.data.repository

import android.util.Log
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.findPartitionPoint
import io.novafoundation.nova.common.utils.hasStorage
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.common.utils.reversed
import io.novafoundation.nova.common.utils.staking
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.api.historicalEras
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.ExposureOverview
import io.novafoundation.nova.feature_staking_api.domain.model.ExposurePage
import io.novafoundation.nova.feature_staking_api.domain.model.PagedExposure
import io.novafoundation.nova.feature_staking_api.domain.model.ValidatorPrefs
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.data.model.Payout
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.ClaimedRewardsPages
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.EraRewardPoints
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindClaimedPages
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindEraRewardPoints
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindExposure
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindExposureOverview
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindExposurePage
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindStakingLedger
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindValidatorPrefs
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.PayoutTarget
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.SubQueryValidatorSetFetcher
import io.novafoundation.nova.feature_staking_impl.data.repository.PayoutRepository.ValidatorEraStake.NominatorInfo
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.multi.MultiQueryBuilder
import io.novafoundation.nova.runtime.storage.source.query.DynamicInstanceBinder
import io.novafoundation.nova.runtime.storage.source.query.metadata
import io.novafoundation.nova.runtime.storage.source.query.multi
import io.novafoundation.nova.runtime.storage.source.query.wrapSingleArgumentKeys
import jp.co.soramitsu.fearless_utils.hash.isPositive
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.StorageEntry
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import java.math.BigInteger

typealias HistoricalMapping<T> = Map<BigInteger, T> // EraIndex -> T

class PayoutRepository(
    private val stakingRepository: StakingRepository,
    private val validatorSetFetcher: SubQueryValidatorSetFetcher,
    private val chainRegistry: ChainRegistry,
    private val remoteStorage: StorageDataSource,
    private val rpcCalls: RpcCalls,
) {

    suspend fun calculateUnpaidPayouts(stakingState: StakingState.Stash): List<Payout> {
        return when (stakingState) {
            is StakingState.Stash.Nominator -> calculateUnpaidPayouts(
                chain = stakingState.chain,
                retrievePayoutTargets = { historicalRange ->
                    validatorSetFetcher.findNominatorPayoutTargets(stakingState.chain, stakingState.stashAddress, historicalRange)
                },
                calculatePayout = {
                    calculateNominatorReward(stakingState.stashId, it)
                }
            )
            is StakingState.Stash.Validator -> calculateUnpaidPayouts(
                chain = stakingState.chain,
                retrievePayoutTargets = { historicalRange ->
                    validatorSetFetcher.findValidatorPayoutTargets(stakingState.chain, stakingState.stashAddress, historicalRange)
                },
                calculatePayout = ::calculateValidatorReward
            )
            else -> throw IllegalStateException("Cannot calculate payouts for ${stakingState::class.simpleName} state")
        }
    }

    private suspend fun calculateUnpaidPayouts(
        chain: Chain,
        retrievePayoutTargets: suspend (historicalRange: List<BigInteger>) -> List<PayoutTarget>,
        calculatePayout: (RewardCalculationContext) -> Payout?
    ): List<Payout> {
        val chainId = chain.id

        val runtime = chainRegistry.getRuntime(chain.id)

        val historicalRange = stakingRepository.historicalEras(chainId)
        val payoutTargets = retrievePayoutTargets(historicalRange)

        Log.d("RX", "Fetched payoutTargets")

        val involvedEras = payoutTargets.map { it.era }.distinct()

        val validatorEraStakes = getValidatorHistoricalStats(chainId, runtime, historicalRange, payoutTargets)

        val historicalTotalEraRewards = retrieveTotalEraReward(chainId, runtime, involvedEras)
        Log.d("RX", "Fetched historicalTotalEraRewards")
        val historicalRewardDistribution = retrieveEraPointsDistribution(chainId, runtime, involvedEras)
        Log.d("RX", "Fetched historicalRewardDistribution")

        val eraClaims = fetchValidatorEraClaims(chain, payoutTargets)
        Log.d("RX", "Fetched eraClaims")

        return validatorEraStakes.mapNotNull { validatorEraStake ->
            val key = validatorEraStake.era to validatorEraStake.stash
            val eraPointsDistribution = historicalRewardDistribution[validatorEraStake.era] ?: return@mapNotNull null

            val rewardCalculationContext = RewardCalculationContext(
                eraStake = validatorEraStake,
                eraClaim = eraClaims[key] ?: return@mapNotNull null,
                totalEraReward = historicalTotalEraRewards[validatorEraStake.era] ?: return@mapNotNull null,
                eraPoints = eraPointsDistribution
            )

            calculatePayout(rewardCalculationContext)
        }.also {
            Log.d("RX", "Constructed payouts")
        }
    }

    private suspend fun partitionHistoricalRangeByPagedExposurePresence(
        historicalRange: List<BigInteger>,
        runtime: RuntimeSnapshot,
        chainId: ChainId
    ): PartitionedHistoricalRange {
        val firstPagedExposureEra = findFirstPagedExposureIndex(historicalRange, runtime, chainId)
            ?: return PartitionedHistoricalRange(legacy = historicalRange, paged = emptyList())

        return PartitionedHistoricalRange(
            legacy = historicalRange.subList(0, firstPagedExposureEra),
            paged = historicalRange.subList(firstPagedExposureEra, historicalRange.size)
        )
    }

    private suspend fun findFirstPagedExposureIndex(
        historicalRange: List<BigInteger>,
        runtime: RuntimeSnapshot,
        chainId: ChainId
    ): Int? {
        val oldestEra = historicalRange.first()

        if (!runtime.metadata.staking().hasStorage("ErasStakersOverview")) return null
        if (!runtime.metadata.staking().hasStorage("ErasStakersClipped")) return 0

        // We expect certain delay between all legacy exposures are gone and runtime upgrade that removes storages completely
        // This small optimizations reduces the number of requests needed to 1 for such period despite adding 1 extra request during migration period
        if (isExposurePaged(oldestEra, runtime, chainId)) return 0

        return historicalRange.findPartitionPoint { era -> isExposurePaged(era, runtime, chainId) }.also {
            Log.d("RX", "Fount first paged exposures era: ${it?.let { historicalRange[it] }}")
        }
    }

    private suspend fun isExposurePaged(eraIndex: EraIndex, runtime: RuntimeSnapshot, chainId: ChainId): Boolean {
        val pagedEraPrefix = runtime.metadata.staking().storage("ErasStakersOverview").storageKey(runtime, eraIndex)
        val storageSize = rpcCalls.getStorageSize(chainId, pagedEraPrefix)

        Log.d("RX", "Fetched storage size for era ${eraIndex}: $storageSize")

        return storageSize.isPositive()
    }

    private suspend fun fetchValidatorEraClaims(
        chain: Chain,
        payoutTargets: List<PayoutTarget>,
    ): Map<Pair<EraIndex, AccountIdKey>, ValidatorEraClaim> {
        return remoteStorage.query(chain.id) {
            var validatorsPrefsDescriptor: MultiQueryBuilder.Descriptor<Pair<EraIndex, AccountIdKey>, ValidatorPrefs?>
            var controllersDescriptor: MultiQueryBuilder.Descriptor<AccountIdKey, AccountIdKey?>
            var claimedPagesDescriptor: MultiQueryBuilder.Descriptor<Pair<EraIndex, AccountIdKey>, ClaimedRewardsPages?>? = null

            val multiQueryResults = multi {
                val eraAndValidatorKeys = payoutTargets.map { listOf(it.era, it.validatorStash.value) }
                val validatorKeys = payoutTargets.mapToSet { it.validatorStash }.map { listOf(it.value) }

                validatorsPrefsDescriptor = runtime.metadata.staking().storage("ErasValidatorPrefs").queryKeys(
                    keysArgs = eraAndValidatorKeys,
                    keyExtractor = { (era: EraIndex, validatorStash: AccountId) -> era to validatorStash.intoKey() },
                    binding = { decoded -> decoded?.let { bindValidatorPrefs(decoded) } }
                )
                controllersDescriptor = runtime.metadata.staking().storage("Bonded").queryKeys(
                    keysArgs = validatorKeys,
                    keyExtractor = { (validatorStash: AccountId) -> validatorStash.intoKey() },
                    binding = { decoded -> decoded?.let { bindAccountId(decoded).intoKey() } }
                )
                if (metadata.staking().hasStorage("ClaimedRewards")) {
                    claimedPagesDescriptor = runtime.metadata.staking().storage("ClaimedRewards").queryKeys(
                        keysArgs = eraAndValidatorKeys,
                        keyExtractor = { (era: EraIndex, validatorStash: AccountId) -> era to validatorStash.intoKey() },
                        binding = { decoded -> decoded?.let { bindClaimedPages(decoded) } }
                    )
                }
            }

            Log.d("RX", "Fetched prefs, controllers and claimed pages")

            val validatorsPrefs = multiQueryResults[validatorsPrefsDescriptor]
            val controllers = multiQueryResults[controllersDescriptor]

            val stashByController = controllers
                .mapValues { (stash, controller) -> controller ?: stash }
                .reversed()

            val claimedLegacyRewards = metadata.staking().storage("Ledger").entries(
                keysArguments = stashByController.keys.map { listOf(it.value) },
                keyExtractor = { (controller: AccountId) -> stashByController.getValue(controller.intoKey()) },
                binding = { decoded, _ -> decoded?.let { bindStakingLedger(decoded).claimedRewards.toSet() } }
            )

            Log.d("RX", "Fetched claimedLegacyRewards")

            val allClaimedPages = claimedPagesDescriptor?.let(multiQueryResults::get)

            payoutTargets.mapNotNull { payoutTarget ->
                val key = payoutTarget.era to payoutTarget.validatorStash

                val prefs = validatorsPrefs[key] ?: return@mapNotNull null
                val claimedLegacyRewardsForValidator = claimedLegacyRewards[payoutTarget.validatorStash] ?: return@mapNotNull null
                val claimedPages = allClaimedPages?.get(key).orEmpty()

                key to ValidatorEraClaim(prefs, claimedLegacyRewardsForValidator, payoutTarget.era, claimedPages)
            }.toMap()
        }
    }

    // Nominator only pays out its own reward page
    private fun calculateNominatorReward(
        nominatorAccountId: AccountId,
        rewardCalculationContext: RewardCalculationContext
    ): Payout? = with(rewardCalculationContext) {
        val nominatorEraInfo = rewardCalculationContext.eraStake.findNominatorInfo(nominatorAccountId) ?: return null
        val nominatorPage = nominatorEraInfo.pageNumber

        if (rewardCalculationContext.eraClaim.pageClaimed(nominatorPage)) return null // already did the payout

        val nominatorStakeInEra = nominatorEraInfo.stake.toDouble()

        val validatorTotalStake = eraStake.totalStake.toDouble()
        val validatorCommission = eraClaim.validatorPrefs.commission.toDouble()

        val validatorTotalReward = calculateValidatorTotalReward(totalEraReward, eraPoints, eraStake.stash.value) ?: return null

        val nominatorReward = validatorTotalReward * (1 - validatorCommission) * (nominatorStakeInEra / validatorTotalStake)
        val nominatorRewardPlanks = nominatorReward.toBigDecimal().toBigInteger()

        Payout(
            validatorStash = rewardCalculationContext.eraStake.stash,
            era = rewardCalculationContext.eraStake.era,
            amount = nominatorRewardPlanks,
            pagesToClaim = listOf(nominatorPage)
        )
    }

    // Validator pays out whole payout
    private fun calculateValidatorReward(
        rewardCalculationContext: RewardCalculationContext
    ): Payout? = with(rewardCalculationContext) {
        val totalPayoutPages = eraStake.totalPages
        val unpaidPages = eraClaim.remainingPagesToClaim(totalPayoutPages)

        if (unpaidPages.isEmpty()) return null

        val validatorTotalStake = eraStake.totalStake.toDouble()
        val validatorOwnStake = eraStake.validatorStake.toDouble()
        val validatorCommission = eraClaim.validatorPrefs.commission.toDouble()

        val validatorTotalReward = calculateValidatorTotalReward(totalEraReward, eraPoints, eraStake.stash.value) ?: return null

        val validatorRewardFromCommission = validatorTotalReward * validatorCommission
        val validatorRewardFromStake = validatorTotalReward * (1 - validatorCommission) * (validatorOwnStake / validatorTotalStake)

        val validatorOwnReward = validatorRewardFromStake + validatorRewardFromCommission
        val validatorRewardPlanks = validatorOwnReward.toBigDecimal().toBigInteger()

        Payout(
            validatorStash = rewardCalculationContext.eraStake.stash,
            era = rewardCalculationContext.eraStake.era,
            amount = validatorRewardPlanks,
            pagesToClaim = unpaidPages
        )
    }

    private fun calculateValidatorTotalReward(
        totalEraReward: BigInteger,
        eraValidatorPointsDistribution: EraRewardPoints,
        validatorAccountId: AccountId,
    ): Double? {
        val totalMinted = totalEraReward.toDouble()

        val totalPoints = eraValidatorPointsDistribution.totalPoints.toDouble()
        val validatorPoints = eraValidatorPointsDistribution.individual.firstOrNull {
            it.accountId.contentEquals(validatorAccountId)
        }?.rewardPoints?.toDouble() ?: return null

        return totalMinted * validatorPoints / totalPoints
    }

    private suspend fun getValidatorHistoricalStats(
        chainId: ChainId,
        runtime: RuntimeSnapshot,
        historicalRange: List<BigInteger>,
        payoutTargets: List<PayoutTarget>,
    ): List<ValidatorEraStake> {
        val (legacyEras, _) = partitionHistoricalRangeByPagedExposurePresence(historicalRange, runtime, chainId)
        val legacyEraSet = legacyEras.toSet()

        val (legacyPayoutTargets, pagedPayoutTargets) = payoutTargets.partition { it.era in legacyEraSet }

        return getLegacyValidatorEraStake(chainId, legacyPayoutTargets) + getPagedValidatorHistoricalStats(chainId, pagedPayoutTargets)
    }

    private suspend fun getPagedValidatorHistoricalStats(
        chainId: ChainId,
        payoutTargets: List<PayoutTarget>,
    ): List<PagedValidatorEraStake> {
        val pagedExposures = remoteStorage.fetchPagedExposures(chainId, payoutTargets)

        return pagedExposures.mapNotNull { (key, pagedExposure) ->
            if (pagedExposure == null) return@mapNotNull null

            val (era, accountId) = key
            PagedValidatorEraStake(accountId, era, pagedExposure)
        }.also {
            Log.d("RX", "Fetched getPagedValidatorHistoricalStats for ${payoutTargets.size} targets")
        }
    }

    private suspend fun StorageDataSource.fetchPagedExposures(
        chainId: ChainId,
        payoutTargets: List<PayoutTarget>
    ): Map<Pair<EraIndex, AccountIdKey>, PagedExposure?> {
        return query(chainId) {
            val eraAndValidatorKeys = payoutTargets.map { listOf(it.era, it.validatorStash.value) }

            val overview = runtime.metadata.staking().storage("ErasStakersOverview").entries(
                keysArguments = eraAndValidatorKeys,
                keyExtractor = { (era: EraIndex, validatorStash: AccountId) -> era to validatorStash.intoKey() },
                binding = { decoded, _ -> decoded?.let { bindExposureOverview(decoded) } },
            )

            val pageKeys = overview.flatMap { (key, exposureOverview) ->
                if (exposureOverview == null) return@flatMap emptyList()

                (0 until exposureOverview.pageCount.toInt()).map { page ->
                    listOf(key.first, key.second.value, page.toBigInteger())
                }
            }


            val pages = runtime.metadata.staking().storage("ErasStakersPaged").entries(
                keysArguments = pageKeys,
                keyExtractor = { (era: EraIndex, validatorStash: AccountId, page: BigInteger) -> Triple(era, validatorStash.intoKey(), page.toInt()) },
                binding = { decoded, _ -> decoded?.let { bindExposurePage(decoded) } },
            )

            mergeIntoPagedExposure(overview, pages)
        }
    }

    private fun mergeIntoPagedExposure(
        overviews: Map<Pair<EraIndex, AccountIdKey>, ExposureOverview?>,
        pages: Map<Triple<EraIndex, AccountIdKey, Int>, ExposurePage?>
    ): Map<Pair<EraIndex, AccountIdKey>, PagedExposure?> {
        return overviews.mapValues { (key, exposureOverview) ->
            if (exposureOverview == null) return@mapValues null

            val totalPages = exposureOverview.pageCount.toInt()
            val exposurePages = (0 until totalPages).map { page ->
                pages[Triple(key.first, key.second, page)] ?: return@mapValues null
            }

            PagedExposure(exposureOverview, exposurePages)
        }
    }

    private suspend fun getLegacyValidatorEraStake(
        chainId: ChainId,
        payoutTargets: List<PayoutTarget>,
    ): List<LegacyValidatorEraStake> {
        return remoteStorage.query(chainId) {
            val eraAndValidatorKeys = payoutTargets.map { listOf(it.era, it.validatorStash.value) }

            val exposuresClipped = runtime.metadata.staking().storage("ErasStakersClipped").entries(
                keysArguments = eraAndValidatorKeys,
                keyExtractor = { (era: EraIndex, validatorStash: AccountId) -> era to validatorStash.intoKey() },
                binding = { decoded, _ -> decoded?.let { bindExposure(decoded) } },
            )

            payoutTargets.mapNotNull { payoutTarget ->
                val eraAndAccountIdKey = payoutTarget.era to payoutTarget.validatorStash

                val exposure = exposuresClipped[eraAndAccountIdKey] ?: return@mapNotNull null

                LegacyValidatorEraStake(payoutTarget.validatorStash, payoutTarget.era, exposure)
            }.also {
                Log.d("RX", "Fetched LegacyValidatorEraStake for ${payoutTargets.size} targets")
            }
        }
    }

    private suspend fun retrieveEraPointsDistribution(
        chainId: ChainId,
        runtime: RuntimeSnapshot,
        eras: List<EraIndex>,
    ): HistoricalMapping<EraRewardPoints> {
        val storage = runtime.metadata.staking().storage("ErasRewardPoints")

        return retrieveHistoricalInfoForValidator(chainId, eras, storage, ::bindEraRewardPoints)
    }

    private suspend fun retrieveTotalEraReward(
        chainId: ChainId,
        runtime: RuntimeSnapshot,
        eras: List<EraIndex>,
    ): HistoricalMapping<BigInteger> {
        val storage = runtime.metadata.staking().storage("ErasValidatorReward")

        return retrieveHistoricalInfoForValidator(chainId, eras, storage, ::bindNumber)
    }

    private suspend fun <T> retrieveHistoricalInfoForValidator(
        chainId: ChainId,
        eras: List<EraIndex>,
        storage: StorageEntry,
        binding: DynamicInstanceBinder<T>,
    ): HistoricalMapping<T> {
        return remoteStorage.query(chainId) {
            storage.entries(
                keysArguments = eras.wrapSingleArgumentKeys(),
                keyExtractor = { (era: EraIndex) -> era },
                binding = { decoded, _ -> binding(decoded) }
            )
        }
    }


    private data class PartitionedHistoricalRange(
        val legacy: List<BigInteger>,
        val paged: List<BigInteger>
    )

    private class RewardCalculationContext(
        val eraStake: ValidatorEraStake,
        val eraClaim: ValidatorEraClaim,
        val eraPoints: EraRewardPoints,
        val totalEraReward: BigInteger
    )

    private interface ValidatorEraStake {

        class NominatorInfo(val stake: Balance, val pageNumber: Int)

        val stash: AccountIdKey

        val era: EraIndex

        val totalStake: BigInteger

        val totalPages: Int

        val validatorStake: BigInteger

        fun findNominatorInfo(accountId: AccountId): NominatorInfo?
    }

    private class LegacyValidatorEraStake(
        override val stash: AccountIdKey,
        override val era: EraIndex,
        private val exposure: Exposure
    ) : ValidatorEraStake {

        override val totalPages: Int = 1

        override val totalStake: BigInteger = exposure.total

        override val validatorStake: BigInteger = exposure.own
        override fun findNominatorInfo(accountId: AccountId): NominatorInfo? {
            val individualExposure = exposure.others.find { it.who.contentEquals(accountId) }

            return individualExposure?.let {
                NominatorInfo(individualExposure.value, pageNumber = 0)
            }
        }
    }

    private class PagedValidatorEraStake(
        override val stash: AccountIdKey,
        override val era: EraIndex,
        private val exposure: PagedExposure,
    ) : ValidatorEraStake {

        override val totalStake: BigInteger = exposure.overview.total

        override val validatorStake: BigInteger = exposure.overview.own

        override val totalPages: Int = exposure.overview.pageCount.toInt()

        override fun findNominatorInfo(accountId: AccountId): NominatorInfo? {
            var result: NominatorInfo? = null

            exposure.pages.forEachIndexed { index, exposurePage ->
                val individualExposure = exposurePage.others.find { it.who.contentEquals(accountId) }

                if (individualExposure != null) {
                    result = NominatorInfo(individualExposure.value, index)
                }
            }

            return result
        }
    }

    private class ValidatorEraClaim(
        val validatorPrefs: ValidatorPrefs,
        legacyClaimedEras: Set<EraIndex>,
        era: EraIndex,
        private val claimedPages: ClaimedRewardsPages
    ) {

        private val claimedFromLegacy = era in legacyClaimedEras

        fun pageClaimed(page: Int): Boolean {
            return claimedFromLegacy || page in claimedPages
        }

        fun remainingPagesToClaim(totalPages: Int): List<Int> {
            if (claimedFromLegacy) return emptyList()

            val allPages = (0 until totalPages).toSet()
            val unpaidPages = allPages - claimedPages

            return unpaidPages.toList()
        }
    }
}
