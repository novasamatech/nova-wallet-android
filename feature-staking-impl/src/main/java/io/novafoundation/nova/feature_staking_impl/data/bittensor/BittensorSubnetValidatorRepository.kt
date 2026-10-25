package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorDirectory
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorDirectoryStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

interface BittensorSubnetValidatorRepository {

    suspend fun validatorCount(chain: Chain, netuid: BittensorNetuid.Subnet): Int

    suspend fun rootValidators(chain: Chain): List<BittensorSubnetValidator>

    suspend fun validateRoot(chain: Chain, validator: BittensorValidator)

    suspend fun validators(chain: Chain, netuid: BittensorNetuid): List<BittensorSubnetValidator>

    suspend fun directory(chain: Chain, netuid: BittensorNetuid): BittensorValidatorDirectory

    suspend fun unrankedDirectory(chain: Chain, netuid: BittensorNetuid): BittensorValidatorDirectory

    suspend fun rankDirectory(chain: Chain, netuid: BittensorNetuid, directory: BittensorValidatorDirectory): BittensorValidatorDirectory

    suspend fun validate(chain: Chain, netuid: BittensorNetuid, validator: BittensorValidator)

    /**
     * [hotkey] as the chain reports it on [netuid], named by the listing when it is listed, without the recommendation gates.
     */
    suspend fun chainValidator(chain: Chain, netuid: BittensorNetuid, hotkey: AccountIdKey): BittensorSubnetValidator

    suspend fun existingPositionValidator(chain: Chain, netuid: BittensorNetuid.Subnet, validator: BittensorValidator): BittensorSubnetValidator {
        return validators(chain, netuid).first { it.validator.hotkey == validator.hotkey && it.canAddToExistingPosition }
    }
}

class RealBittensorSubnetValidatorRepository(
    private val backend: BittensorValidatorBackend,
    private val storage: StorageDataSource,
    private val rpc: RpcCalls
) : BittensorSubnetValidatorRepository {

    override suspend fun validatorCount(chain: Chain, netuid: BittensorNetuid.Subnet): Int {
        val hash = rpc.getBlockHash(chain.id)
        return storage.query(chain.id, at = hash) {
            runtime.metadata.module("SubtensorModule").storage("ValidatorPermit")
                .query(netuid.value.toBigInteger(), binding = ::bindBittensorValidatorCount)
        }
    }

    override suspend fun rootValidators(chain: Chain): List<BittensorSubnetValidator> = validators(chain, BittensorNetuid.Root)

    override suspend fun validateRoot(chain: Chain, validator: BittensorValidator) = validate(chain, BittensorNetuid.Root, validator)

    /** Selection does not depend on the recommendations: a failed ranking leaves the rows unranked but selectable. */
    override suspend fun validators(chain: Chain, netuid: BittensorNetuid): List<BittensorSubnetValidator> {
        return directory(chain, netuid).validators
    }

    override suspend fun directory(chain: Chain, netuid: BittensorNetuid): BittensorValidatorDirectory = withTimeoutOrNull(REQUEST_TIMEOUT_MILLIS) {
        val listing = loadListing(chain, netuid)
        val ranking = loadRanking(netuid)
        val snapshot = loadDirectorySnapshot(chain, netuid, listing.candidates)
        val validators = mapDirectoryRows(chain, netuid, listing.candidates, snapshot, ranking.getOrNull())
        BittensorValidatorDirectory(validators, listing.status, ranking.exceptionOrNull(), ranking.getOrNull().isStale())
    } ?: throw BittensorTimeoutException("Validator directory")

    override suspend fun unrankedDirectory(chain: Chain, netuid: BittensorNetuid): BittensorValidatorDirectory =
        withTimeoutOrNull(REQUEST_TIMEOUT_MILLIS) {
            val listing = loadListing(chain, netuid)
            val snapshot = loadDirectorySnapshot(chain, netuid, listing.candidates)
            val rows = mapBittensorValidators(chain, netuid, listing.candidates, snapshot)
            BittensorValidatorDirectory(rows, listing.status, recommendationFailure = null)
        } ?: throw BittensorTimeoutException("Validator directory")

    /** Root lists the root validators resource; a subnet lists its own validator directory. */
    private suspend fun loadListing(chain: Chain, netuid: BittensorNetuid): DirectoryListing = if (netuid.isRoot()) {
        val response = backend.rootValidators()
        DirectoryListing(mapBittensorRootValidators(chain, response), mapBittensorRootDirectoryStatus(response.meta))
    } else {
        val response = backend.validators(netuid.value)
        DirectoryListing(mapCandidates(chain, response), mapBittensorDirectoryStatus(response.meta))
    }

    override suspend fun rankDirectory(
        chain: Chain,
        netuid: BittensorNetuid,
        directory: BittensorValidatorDirectory
    ): BittensorValidatorDirectory = withTimeoutOrNull(REQUEST_TIMEOUT_MILLIS) {
        val ranking = loadRanking(netuid).getOrThrow()
        val candidates = directory.validators.map { it.validator }
        // Retry only ranking and its live eligibility checks, never the independent directory HTTP request.
        val snapshot = loadDirectorySnapshot(chain, netuid, candidates)
        val rows = mapDirectoryRows(chain, netuid, candidates, snapshot, ranking)
        directory.copy(validators = rows, recommendationFailure = null, staleRecommendations = ranking.isStale())
    } ?: throw BittensorTimeoutException("Validator eligibility checks")

    private suspend fun loadDirectorySnapshot(
        chain: Chain,
        netuid: BittensorNetuid,
        candidates: List<BittensorValidator>
    ): BittensorValidatorSnapshot {
        val hash = rpc.getBlockHash(chain.id)
        return storage.query(chain.id, at = hash, applyStorageDefault = true) {
            loadBittensorValidatorSnapshot(netuid, candidates, includeRegistration = true)
        }
    }

    private fun mapDirectoryRows(
        chain: Chain,
        netuid: BittensorNetuid,
        candidates: List<BittensorValidator>,
        snapshot: BittensorValidatorSnapshot,
        ranking: BittensorRankedValidatorsRemote?
    ): List<BittensorSubnetValidator> {
        // A generation older than the subnet's registration recommends nothing; nor do its names apply.
        val rankedRows = ranking?.takeUnless { bittensorRankingPredatesRegistration(netuid, snapshot, it) }?.items.orEmpty()
        // A STALE generation still recommends: the recommendations contract makes it usable data, shown as outdated.
        val recommendationRanks = recommendationRanks(rankedRows)
        // Root rows keep the names of the root list.
        val namedCandidates = if (netuid.isRoot()) candidates else applyRankedNames(chain, candidates, rankedRows)
        val gates = ranking?.let { mapBittensorValidatorGates(BittensorRecommendationsRemote(it.meta)) }
        return mapBittensorValidators(chain, netuid, namedCandidates, snapshot)
            .map { row ->
                // The take limit and the other gates decide only which validators are recommended.
                val rank = recommendationRanks[row.address]
                    ?.takeIf { gates != null && snapshot.passesBittensorRecommendationGates(netuid, row.validator.hotkey, gates) }
                row.copy(recommended = rank != null, recommendationRank = rank)
            }
            .sortedWith(
                compareByDescending<BittensorSubnetValidator> { it.canSelect }
                    .thenByDescending { it.recommended }
                    .thenBy { it.validator.name }
            )
    }

    private suspend fun loadRanking(netuid: BittensorNetuid): Result<BittensorRankedValidatorsRemote> = try {
        Result.success(withTimeout(RANKING_TIMEOUT_MILLIS) { backend.rankedValidators(netuid.value) })
    } catch (exception: TimeoutCancellationException) {
        currentCoroutineContext().ensureActive()
        Result.failure(BittensorTimeoutException("Validator recommendations", exception))
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception)
    }

    /** Recommended hotkeys by their position in the ranking, which breaks automatic preset ties. */
    private fun recommendationRanks(rows: List<BittensorRankedValidatorsRemote.Item>): Map<String, Int> = rows
        .filter { it.eligible == true && it.recommendationClass in RECOMMENDATION_CLASSES }
        .mapNotNull { it.hotkey }
        .distinct()
        .withIndex()
        .associate { (rank, hotkey) -> hotkey to rank }

    private fun BittensorRankedValidatorsRemote?.isStale(): Boolean = this != null && !hasFreshBittensorRecommendations(meta)

    private fun applyRankedNames(
        chain: Chain,
        candidates: List<BittensorValidator>,
        rows: List<BittensorRankedValidatorsRemote.Item>
    ): List<BittensorValidator> {
        val rankedNames = rows.associateBy { it.hotkey }
        return candidates.map { validator ->
            val address = chain.addressOf(validator.hotkey.value)
            val name = rankedNames[address]?.validatorName
                ?.trim()
                ?.takeIf(String::isNotBlank)
            validator.copy(name = name ?: validator.name)
        }
    }

    private fun mapCandidates(chain: Chain, response: BittensorValidatorsRemote): List<BittensorValidator> {
        val status = mapBittensorDirectoryStatus(response.meta)
        val hasUsableStake = hasUsableBittensorDirectoryStake(status)
        return requireNotNull(response.items)
            .mapNotNull { mapValidator(chain, it, hasUsableStake) }
            .distinctBy { it.hotkey }
            .take(MAX_VALIDATORS)
    }

    private fun mapValidator(chain: Chain, remote: BittensorValidatorsRemote.Item, hasUsableStake: Boolean): BittensorValidator? {
        val address = remote.hotkey ?: return null
        val hotkey = runCatching { chain.accountIdOf(address) }
            .getOrNull() ?: return null
        val identityName = remote.identitySourceName
            ?.trim()
            ?.takeIf(String::isNotBlank)
        val stakeName = remote.stakeSourceName
            ?.trim()
            ?.takeIf(String::isNotBlank)

        val reportedStake = remote.reportedMeasurements?.validatorStake
            ?.takeIf { hasUsableStake }
            ?.toBigDecimalOrNull()
            ?.takeIf { it.isNonNegative }

        return BittensorValidator(AccountIdKey(hotkey), identityName ?: stakeName ?: address, reportedStake)
    }

    /** Only an inactive validator cannot take a new stake; the take limit gates recommendations alone. */
    override suspend fun validate(chain: Chain, netuid: BittensorNetuid, validator: BittensorValidator) = withTimeoutOrNull(REQUEST_TIMEOUT_MILLIS) {
        val row = enrich(chain, netuid, listOf(validator)).single()
        require(row.canSelect) { "Validator is no longer eligible in this subnet" }
    } ?: throw BittensorTimeoutException("Validator eligibility checks")

    override suspend fun chainValidator(chain: Chain, netuid: BittensorNetuid, hotkey: AccountIdKey): BittensorSubnetValidator {
        val validator = listedValidator(chain, netuid, hotkey) ?: BittensorValidator(hotkey, chain.addressOf(hotkey))
        val snapshot = withTimeoutOrNull(REQUEST_TIMEOUT_MILLIS) {
            val hash = rpc.getBlockHash(chain.id)
            storage.query(chain.id, at = hash, applyStorageDefault = true) {
                loadBittensorValidatorSnapshot(netuid, listOf(validator))
            }
        } ?: throw BittensorTimeoutException("Validator eligibility checks")
        return mapBittensorValidators(chain, netuid, listOf(validator), snapshot).single()
    }

    /** The listing only names the validator, so an unreadable listing leaves it unnamed. */
    private suspend fun listedValidator(chain: Chain, netuid: BittensorNetuid, hotkey: AccountIdKey): BittensorValidator? = try {
        withTimeoutOrNull(REQUEST_TIMEOUT_MILLIS) {
            loadListing(chain, netuid).candidates.firstOrNull { it.hotkey == hotkey }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        null
    }

    override suspend fun existingPositionValidator(
        chain: Chain,
        netuid: BittensorNetuid.Subnet,
        validator: BittensorValidator
    ): BittensorSubnetValidator = withTimeout(REQUEST_TIMEOUT_MILLIS) {
        val directory = backend.validators(netuid.value)
        val identifiedValidator = mapCandidates(chain, directory).firstOrNull { it.hotkey == validator.hotkey } ?: validator
        enrich(chain, netuid, listOf(identifiedValidator)).single()
    }

    private suspend fun enrich(
        chain: Chain,
        netuid: BittensorNetuid,
        validators: List<BittensorValidator>
    ): List<BittensorSubnetValidator> {
        if (validators.isEmpty()) return emptyList()
        val hash = rpc.getBlockHash(chain.id)
        val snapshot = storage.query(
            chain.id,
            at = hash,
            applyStorageDefault = true
        ) {
            loadBittensorValidatorSnapshot(netuid, validators)
        }
        return mapBittensorValidators(
            chain,
            netuid,
            validators,
            snapshot
        )
    }

    private class DirectoryListing(val candidates: List<BittensorValidator>, val status: BittensorValidatorDirectoryStatus)

    private companion object {

        val RECOMMENDATION_CLASSES = setOf("stable", "balanced", "higherUpside")

        const val MAX_VALIDATORS = 512

        const val REQUEST_TIMEOUT_MILLIS = 45_000L

        const val RANKING_TIMEOUT_MILLIS = 15_000L
    }
}

internal fun bindBittensorValidatorCount(value: Any?): Int = bindList(value, ::bindBoolean).count { it }

/** Whether [ranking] was computed for an earlier registration of the subnet, or the registration block is unreadable. */
internal fun bittensorRankingPredatesRegistration(
    netuid: BittensorNetuid,
    snapshot: BittensorValidatorSnapshot,
    ranking: BittensorRankedValidatorsRemote
): Boolean {
    if (netuid.isRoot()) return false
    val registeredAt = snapshot.registeredAt ?: return true
    return registeredAt > mapBittensorRecommendationGeneration(ranking.meta).sourceBlockNumber
}
