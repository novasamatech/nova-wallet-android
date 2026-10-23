package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorAlphaYieldRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorBackend
import io.novafoundation.nova.feature_staking_impl.data.bittensor.mapBittensorRootValidatorNames
import io.novafoundation.nova.feature_staking_impl.data.bittensor.mapBittensorRootYields
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.runtime.ext.accountIdOf
import java.math.BigDecimal
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class BittensorValidatorInteractor(
    private val validatorRepository: BittensorSubnetValidatorRepository,
    private val chainRegistry: ChainRegistry,
    private val alphaYields: BittensorAlphaYieldRepository,
    private val backend: BittensorValidatorBackend
) {

    suspend fun chain(): Chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)

    /** Presentation only: eligibility is checked by selection and amount entry, never by discovery. */
    suspend fun previews(netuid: BittensorNetuid.Subnet, selectedAddress: String?): List<BittensorValidatorPreview> {
        val chain = chain()
        if (selectedAddress != null) {
            val hotkey = AccountIdKey(chain.accountIdOf(selectedAddress))
            val validator = BittensorValidator(hotkey, validatorName(netuid, hotkey) ?: selectedAddress)
            return listOf(BittensorValidatorPreview(validator, canPreselect = true))
        }
        // A STALE generation still recommends: the recommendations contract makes it usable data.
        val response = backend.rankedValidators(netuid.value)
        val recommended = response.items.orEmpty().mapNotNull { row ->
            mapBittensorValidatorPreview(chain, row)
        }
        if (recommended.isNotEmpty()) return recommended.map { BittensorValidatorPreview(it, canPreselect = true) }

        val directory = backend.validators(netuid.value)
        return directory.items.orEmpty().mapNotNull { row ->
            mapBittensorDirectoryPreview(chain, row)?.let { BittensorValidatorPreview(it, canPreselect = false) }
        }
    }

    suspend fun validatorName(netuid: BittensorNetuid, hotkey: AccountIdKey): String? {
        return validatorNames(netuid, setOf(hotkey))[hotkey]
    }

    /** Directory names of [hotkeys]; hotkeys without a name are absent, callers fall back to the address. */
    suspend fun validatorNames(netuid: BittensorNetuid, hotkeys: Set<AccountIdKey>): Map<AccountIdKey, String> {
        if (hotkeys.isEmpty()) return emptyMap()
        val chain = chain()
        // Root names come from the root list, so positions and exits name a root validator as the list does.
        if (netuid.isRoot()) return mapBittensorRootValidatorNames(chain, backend.rootValidators()).filterKeys { it in hotkeys }
        val response = backend.validators(netuid.value)
        return buildMap {
            response.items.orEmpty().forEach { row ->
                val hotkey = row.hotkey?.let { runCatching { AccountIdKey(chain.accountIdOf(it)) }.getOrNull() } ?: return@forEach
                if (hotkey !in hotkeys || hotkey in this) return@forEach
                val name = row.identitySourceName?.takeIf { it.isNotBlank() } ?: row.stakeSourceName?.takeIf { it.isNotBlank() }
                name?.let { put(hotkey, it) }
            }
        }
    }

    suspend fun yields(netuid: BittensorNetuid, includeUnreliable: Boolean = false): Map<AccountIdKey, BigDecimal> {
        val chain = chain()
        if (netuid is BittensorNetuid.Subnet) return alphaYields.alphaYields(chain, netuid, includeUnreliable)
        return mapBittensorRootYields(chain, backend.rootValidators())
    }

    suspend fun validators(netuid: BittensorNetuid): List<BittensorSubnetValidator> {
        val chain = chain()
        return validatorRepository.validators(chain, netuid)
    }

    suspend fun directory(netuid: BittensorNetuid): BittensorValidatorDirectory {
        val chain = chain()
        return validatorRepository.unrankedDirectory(chain, netuid)
    }

    suspend fun rankDirectory(netuid: BittensorNetuid, directory: BittensorValidatorDirectory): BittensorValidatorDirectory {
        val chain = chain()
        return validatorRepository.rankDirectory(chain, netuid, directory)
    }
}
