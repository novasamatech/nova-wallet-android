package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorDirectory
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorGates
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validatorCommissionRateFromU16
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

    override suspend fun validators(chain: Chain, netuid: BittensorNetuid): List<BittensorSubnetValidator> {
        val directory = directory(chain, netuid)
        // List-only consumers cannot represent partial success. Preserve their standard retry path.
        directory.recommendationFailure?.let { throw it }
        return directory.validators
    }

    override suspend fun directory(chain: Chain, netuid: BittensorNetuid): BittensorValidatorDirectory = withTimeoutOrNull(REQUEST_TIMEOUT_MILLIS) {
        val response = backend.validators(netuid.value)
        val candidates = mapCandidates(chain, response)
        val ranking = loadRanking(netuid)
        val snapshot = loadDirectorySnapshot(chain, netuid, candidates)
        val admittedRanking = ranking.mapCatching { ranked ->
            requireBittensorRegistrationFreshness(netuid, snapshot, ranked)
            ranked
        }
        val validators = mapDirectoryRows(chain, netuid, candidates, snapshot, admittedRanking.getOrNull())
        BittensorValidatorDirectory(validators, mapBittensorDirectoryStatus(response.meta), admittedRanking.exceptionOrNull())
    } ?: throw BittensorTimeoutException("Validator directory")

    override suspend fun unrankedDirectory(chain: Chain, netuid: BittensorNetuid): BittensorValidatorDirectory =
        withTimeoutOrNull(REQUEST_TIMEOUT_MILLIS) {
            val response = backend.validators(netuid.value)
            val candidates = mapCandidates(chain, response)
            val snapshot = loadDirectorySnapshot(chain, netuid, candidates)
            val rows = mapUnavailablePolicyDirectory(chain, netuid, candidates, snapshot)
            BittensorValidatorDirectory(rows, mapBittensorDirectoryStatus(response.meta), recommendationFailure = null)
        } ?: throw BittensorTimeoutException("Validator directory")

    override suspend fun rankDirectory(
        chain: Chain,
        netuid: BittensorNetuid,
        directory: BittensorValidatorDirectory
    ): BittensorValidatorDirectory = withTimeoutOrNull(REQUEST_TIMEOUT_MILLIS) {
        val ranking = loadRanking(netuid).getOrThrow()
        val candidates = directory.validators.map { it.validator }
        // Retry only ranking and its live eligibility checks, never the independent directory HTTP request.
        val snapshot = loadDirectorySnapshot(chain, netuid, candidates)
        requireBittensorRegistrationFreshness(netuid, snapshot, ranking)
        val rows = mapDirectoryRows(chain, netuid, candidates, snapshot, ranking)
        directory.copy(validators = rows, recommendationFailure = null)
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
        val rankedRows = ranking?.items.orEmpty()
        val recommendedHotkeys = if (hasFreshBittensorRecommendations(ranking?.meta)) recommendedHotkeys(rankedRows) else emptySet()
        val namedCandidates = applyRankedNames(chain, candidates, rankedRows)
        val rows = ranking?.let {
            val gates = mapBittensorValidatorGates(BittensorRecommendationsRemote(it.meta))
            mapBittensorValidators(chain, netuid, namedCandidates, gates, snapshot)
        } ?: mapUnavailablePolicyDirectory(chain, netuid, candidates, snapshot)
        return rows.map { row -> row.copy(recommended = row.canSelect && row.address in recommendedHotkeys) }
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

    private fun mapUnavailablePolicyDirectory(
        chain: Chain,
        netuid: BittensorNetuid,
        candidates: List<BittensorValidator>,
        snapshot: BittensorValidatorSnapshot
    ): List<BittensorSubnetValidator> = candidates.mapNotNull { validator ->
        val take = snapshot.commissionsInU16[validator.hotkey] ?: return@mapNotNull null
        BittensorSubnetValidator(
            validator,
            chain.addressOf(validator.hotkey),
            validatorCommissionRateFromU16(take),
            selectable = false,
            inactive = snapshot.isInactive(netuid, validator.hotkey),
            recommendationsAvailable = false
        )
    }

    private fun recommendedHotkeys(rows: List<BittensorRankedValidatorsRemote.Item>): Set<String> = rows
        .filter { it.eligible == true && it.recommendationClass in RECOMMENDATION_CLASSES }
        .mapNotNull { it.hotkey }
        .toSet()

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
        val hasFreshStake = hasFreshBittensorDirectoryStake(status)
        return requireNotNull(response.items)
            .mapNotNull { mapValidator(chain, it, hasFreshStake) }
            .distinctBy { it.hotkey }
            .take(MAX_VALIDATORS)
    }

    private fun mapValidator(chain: Chain, remote: BittensorValidatorsRemote.Item, hasFreshStake: Boolean): BittensorValidator? {
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
            ?.takeIf { hasFreshStake }
            ?.toBigDecimalOrNull()
            ?.takeIf { it.isNonNegative }

        return BittensorValidator(AccountIdKey(hotkey), identityName ?: stakeName ?: address, reportedStake)
    }

    override suspend fun validate(chain: Chain, netuid: BittensorNetuid, validator: BittensorValidator) = withTimeoutOrNull(REQUEST_TIMEOUT_MILLIS) {
        val gates = backend.gates()
        val row = enrich(
            chain,
            netuid,
            listOf(validator),
            gates
        )
            .singleOrNull()
        require(row?.canSelect == true) { "Validator is no longer eligible in this subnet" }
    } ?: throw BittensorTimeoutException("Validator eligibility checks")

    override suspend fun existingPositionValidator(
        chain: Chain,
        netuid: BittensorNetuid.Subnet,
        validator: BittensorValidator
    ): BittensorSubnetValidator = withTimeout(REQUEST_TIMEOUT_MILLIS) {
        val directory = backend.validators(netuid.value)
        val identifiedValidator = mapCandidates(chain, directory).firstOrNull { it.hotkey == validator.hotkey } ?: validator
        val gates = backend.gates()
        val row = enrich(chain, netuid, listOf(identifiedValidator), gates).singleOrNull()
        requireNotNull(row?.takeIf { it.canAddToExistingPosition }) { "Validator is no longer eligible for additional stake" }
    }

    private suspend fun enrich(
        chain: Chain,
        netuid: BittensorNetuid,
        validators: List<BittensorValidator>,
        gates: BittensorValidatorGates
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
            gates,
            snapshot
        )
    }
    private companion object {

        val RECOMMENDATION_CLASSES = setOf("stable", "balanced", "higherUpside")

        const val MAX_VALIDATORS = 512

        const val REQUEST_TIMEOUT_MILLIS = 45_000L

        const val RANKING_TIMEOUT_MILLIS = 15_000L
    }
}

internal fun bindBittensorValidatorCount(value: Any?): Int = bindList(value, ::bindBoolean).count { it }

internal fun requireBittensorRegistrationFreshness(
    netuid: BittensorNetuid,
    snapshot: BittensorValidatorSnapshot,
    ranking: BittensorRankedValidatorsRemote
) {
    if (netuid.isRoot()) return
    val registeredAt = requireNotNull(snapshot.registeredAt) { "Subnet registration is unavailable" }
    val generation = mapBittensorRecommendationGeneration(ranking.meta)
    require(registeredAt <= generation.sourceBlockNumber) { "Recommendations predate subnet registration" }
}
