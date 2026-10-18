package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorGates
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import kotlinx.coroutines.withTimeout

interface BittensorSubnetValidatorRepository {

    suspend fun rootValidators(chain: Chain): List<BittensorSubnetValidator>

    suspend fun validateRoot(chain: Chain, validator: BittensorValidator)

    suspend fun validators(chain: Chain, netuid: BittensorNetuid): List<BittensorSubnetValidator>

    suspend fun validate(chain: Chain, netuid: BittensorNetuid, validator: BittensorValidator)
}

class RealBittensorSubnetValidatorRepository(
    private val backend: BittensorValidatorBackend,
    private val storage: StorageDataSource,
    private val rpc: RpcCalls
) : BittensorSubnetValidatorRepository {

    override suspend fun rootValidators(chain: Chain): List<BittensorSubnetValidator> = validators(chain, BittensorNetuid.Root)

    override suspend fun validateRoot(chain: Chain, validator: BittensorValidator) = validate(chain, BittensorNetuid.Root, validator)

    override suspend fun validators(chain: Chain, netuid: BittensorNetuid): List<BittensorSubnetValidator> = withTimeout(REQUEST_TIMEOUT_MILLIS) {
        val ranking = backend.rankedValidators(netuid.value)
        val gates = mapBittensorValidatorGates(BittensorRecommendationsRemote(ranking.meta))
        val rankedRows = requireNotNull(ranking.items)
        val recommendedHotkeys = recommendedHotkeys(rankedRows)
        val response = backend.validators(netuid.value)
        val candidates = mapCandidates(chain, response)
        val namedCandidates = applyRankedNames(chain, candidates, rankedRows)
        enrich(
            chain,
            netuid,
            namedCandidates,
            gates
        ).map { row -> row.copy(recommended = row.selectable && row.address in recommendedHotkeys) }
            .sortedWith(
                compareByDescending<BittensorSubnetValidator> { it.selectable }
                    .thenByDescending { it.recommended }
                    .thenBy { it.validator.name }
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

    private fun mapCandidates(chain: Chain, response: BittensorValidatorsRemote): List<BittensorValidator> = requireNotNull(response.items)
        .mapNotNull { mapValidator(chain, it) }
        .distinctBy { it.hotkey }
        .take(MAX_VALIDATORS)

    private fun mapValidator(chain: Chain, remote: BittensorValidatorsRemote.Item): BittensorValidator? {
        val address = remote.hotkey ?: return null
        val hotkey = runCatching { chain.accountIdOf(address) }
            .getOrNull() ?: return null
        val identityName = remote.identitySourceName
            ?.trim()
            ?.takeIf(String::isNotBlank)
        val stakeName = remote.stakeSourceName
            ?.trim()
            ?.takeIf(String::isNotBlank)

        return BittensorValidator(AccountIdKey(hotkey), identityName ?: stakeName ?: address)
    }

    override suspend fun validate(chain: Chain, netuid: BittensorNetuid, validator: BittensorValidator) = withTimeout(REQUEST_TIMEOUT_MILLIS) {
        val gates = backend.gates()
        val row = enrich(
            chain,
            netuid,
            listOf(validator),
            gates
        )
            .singleOrNull()
        require(row?.selectable == true) { "Validator is no longer eligible in this subnet" }
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
    }
}
