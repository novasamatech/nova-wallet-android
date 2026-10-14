package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorGates
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import kotlinx.coroutines.withTimeout
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

interface BittensorSubnetValidatorRepository {

    suspend fun rootValidators(chain: Chain): List<BittensorSubnetValidator>

    suspend fun validateRoot(chain: Chain, validator: BittensorValidator)

    suspend fun validators(chain: Chain, subnetId: BittensorSubnetId): List<BittensorSubnetValidator>

    suspend fun validate(chain: Chain, subnetId: BittensorSubnetId, validator: BittensorValidator)
}

class RealBittensorSubnetValidatorRepository(
    private val backend: BittensorValidatorBackend,
    private val storage: StorageDataSource,
    private val rpc: RpcCalls
) : BittensorSubnetValidatorRepository {

    override suspend fun rootValidators(chain: Chain): List<BittensorSubnetValidator> = validators(chain, 0)

    override suspend fun validateRoot(chain: Chain, validator: BittensorValidator) = validate(chain, 0, validator)

    override suspend fun validators(chain: Chain, subnetId: BittensorSubnetId): List<BittensorSubnetValidator> = validators(chain, subnetId.value)

    private suspend fun validators(chain: Chain, netuid: Int): List<BittensorSubnetValidator> = withTimeout(45_000L) {
        val gates = backend.gates()
        val candidates = requireNotNull(backend.validators(netuid).items).filter { it.netuid == netuid }
            .mapNotNull { row ->
                val address = row.hotkey ?: return@mapNotNull null
                val hotkey = runCatching { chain.accountIdOf(address) }.getOrNull() ?: return@mapNotNull null
                BittensorValidator(
                    AccountIdKey(hotkey),
                    row.identitySourceName?.trim()?.takeIf(String::isNotBlank)
                        ?: row.stakeSourceName?.trim()?.takeIf(String::isNotBlank) ?: address,
                    null
                )
            }.distinctBy { it.hotkey }.take(512)
        enrich(chain, netuid, candidates, gates)
            .sortedWith(compareByDescending<BittensorSubnetValidator> { it.selectable }.thenBy { it.validator.name })
    }

    override suspend fun validate(chain: Chain, subnetId: BittensorSubnetId, validator: BittensorValidator) = validate(chain, subnetId.value, validator)

    private suspend fun validate(chain: Chain, netuid: Int, validator: BittensorValidator) = withTimeout(45_000L) {
        val gates = backend.gates()
        val row = enrich(chain, netuid, listOf(validator), gates).singleOrNull()
        require(row?.selectable == true) { "Validator is no longer eligible in this subnet" }
    }

    private suspend fun enrich(
        chain: Chain,
        subnetId: Int,
        validators: List<BittensorValidator>,
        gates: BittensorValidatorGates
    ): List<BittensorSubnetValidator> {
        if (validators.isEmpty()) return emptyList()
        val hash = rpc.getBlockHash(chain.id)
        return storage.query(chain.id, at = hash, applyStorageDefault = true) {
            val pallet = runtime.metadata.module("SubtensorModule")
            val netuid = BigInteger.valueOf(subnetId.toLong())
            val isRoot = subnetId == 0
            val permits = if (!isRoot && gates.requirePermit) pallet.storage("ValidatorPermit").query(netuid) { bindList(it, ::bindBoolean) } else emptyList()
            val updates = if (!isRoot && gates.requireActive) pallet.storage("LastUpdate").query(netuid) { bindList(it, ::bindNumber) } else emptyList()
            val activeThreshold = if (!isRoot && gates.requireActive) {
                val block = runtime.metadata.module("System").storage("Number").query(binding = ::bindNumber)
                val tempo = pallet.storage("Tempo").query(netuid, binding = ::bindNumber)
                val factor = pallet.storage("ActivityCutoffFactorMilli").query(netuid, binding = ::bindNumber)
                block - (tempo * factor / BigInteger.valueOf(1000)).max(BigInteger.ONE)
            } else {
                BigInteger.ZERO
            }
            val uids = pallet.storage("Uids").entries(
                keysArguments = validators.map { listOf(netuid, it.hotkey.value) },
                keyExtractor = { key -> AccountIdKey(key.component2()) },
                binding = { value, _ -> value?.let(::bindBittensorU16) }
            )
            val takes = pallet.storage("Delegates").entries(
                keysArguments = validators.map { listOf(it.hotkey.value) },
                keyExtractor = { key -> AccountIdKey(key.component1()) },
                binding = { value, _ -> value?.let(::bindNumber) }
            )
            validators.mapNotNull { validator ->
                val uid = uids[validator.hotkey] ?: return@mapNotNull null
                val take = takes[validator.hotkey] ?: return@mapNotNull null
                require(uid in 0..65535)
                if (!gates.allowsTake(take)) return@mapNotNull null
                if (!isRoot && gates.requirePermit && permits.getOrNull(uid) != true) return@mapNotNull null
                val active = isRoot || !gates.requireActive || updates.getOrNull(uid)?.let { it >= activeThreshold } == true
                BittensorSubnetValidator(
                    validator,
                    chain.addressOf(validator.hotkey),
                    take.toBigDecimal().divide(BigDecimal(65535), 12, RoundingMode.HALF_EVEN),
                    active
                )
            }
        }
    }
}
