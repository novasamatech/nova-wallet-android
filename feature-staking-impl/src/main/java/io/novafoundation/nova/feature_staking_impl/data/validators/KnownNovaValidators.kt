package io.novafoundation.nova.feature_staking_impl.data.validators

import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import io.novasama.substrate_sdk_android.extensions.toHexString
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface KnownNovaValidators {

    suspend fun getValidatorIds(chainId: ChainId): List<String>
}

class RemoteKnownNovaValidators(
    private val validatorsApi: NovaValidatorsApi,
    private val chainRegistry: ChainRegistry,
) : KnownNovaValidators {

    private var validatorsByNetwork: Map<String, List<String>>? = null
    private val validatorsMutex = Mutex()

    override suspend fun getValidatorIds(chainId: ChainId): List<String> {
        return getValidators()[chainId].orEmpty()
    }

    private suspend fun getValidators(): Map<String, List<String>> {
        return validatorsMutex.withLock {
            if (validatorsByNetwork == null) {
                validatorsByNetwork = fetchValidators()
            }

            requireNotNull(validatorsByNetwork)
        }
    }

    private suspend fun fetchValidators(): Map<String, List<String>> {
        return runCatching {
            val chainsById = chainRegistry.chainsById()

            validatorsApi.getValidators().mapValues { (chainId, addresses) ->
                chainsById[chainId]?.let { chain ->
                    addresses.convertAddressesToAccountIds(chain)
                }
            }.filterNotNull()
        }.getOrDefault(emptyMap())
    }

    private fun List<String>.convertAddressesToAccountIds(chain: Chain): List<String> {
        return mapNotNull {
            chain.tryConvertAddressToAccountIdHex(it)
        }
    }

    private fun Chain.tryConvertAddressToAccountIdHex(address: String): String? {
        return runCatching { accountIdOf(address).toHexString() }.getOrNull()
    }
}
