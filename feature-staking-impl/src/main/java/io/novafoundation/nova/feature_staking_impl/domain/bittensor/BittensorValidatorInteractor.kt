package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorAlphaYieldRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorBackend
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.common.utils.isNonNegative
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
        val chain = chain()
        val response = backend.validators(netuid.value)
        val row = response.items.orEmpty().firstOrNull { item ->
            item.hotkey?.let { runCatching { AccountIdKey(chain.accountIdOf(it)) }.getOrNull() } == hotkey
        } ?: return null
        return row.identitySourceName?.takeIf { it.isNotBlank() }
            ?: row.stakeSourceName?.takeIf { it.isNotBlank() }
    }

    suspend fun yields(netuid: BittensorNetuid): Map<AccountIdKey, BigDecimal> {
        val chain = chain()
        if (netuid is BittensorNetuid.Subnet) return alphaYields.alphaYields(chain, netuid)
        val response = backend.rootValidators()
        return buildMap {
            response.items.orEmpty().forEach { row ->
                val stake = row.rootStakeTao?.toBigDecimalOrNull() ?: return@forEach
                if (!stake.isPositive) return@forEach
                val rate = row.stakeYield?.d30?.apy?.toBigDecimalOrNull()?.takeIf { it.isNonNegative } ?: return@forEach
                val address = row.hotkey ?: return@forEach
                val hotkey = runCatching { AccountIdKey(chain.accountIdOf(address)) }.getOrNull() ?: return@forEach
                put(hotkey, rate)
            }
        }
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
