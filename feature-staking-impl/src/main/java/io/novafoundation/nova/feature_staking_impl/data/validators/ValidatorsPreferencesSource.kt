package io.novafoundation.nova.feature_staking_impl.data.validators

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.mapNotNullToSet
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.common.utils.mapValuesNotNull
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.toHexString
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// TODO migrate this to use AccountIdKey instead of hex-encoded account id
interface ValidatorsPreferencesSource {

    suspend fun getRecommendedValidatorIds(chainId: ChainId): Set<String>

    suspend fun getExcludedValidatorIds(chainId: ChainId): Set<String>
}

suspend fun ValidatorsPreferencesSource.getRecommendedValidatorIdKeys(chainId: ChainId): Set<AccountIdKey> {
    return getRecommendedValidatorIds(chainId).mapToSet { it.fromHex().intoKey() }
}

suspend fun ValidatorsPreferencesSource.getExcludedValidatorIdKeys(chainId: ChainId): Set<AccountIdKey> {
    return getExcludedValidatorIds(chainId).mapToSet { it.fromHex().intoKey() }
}

class RemoteValidatorsPreferencesSource(
    private val validatorsApi: NovaValidatorsApi,
    private val chainRegistry: ChainRegistry,
) : ValidatorsPreferencesSource {

    private var validatorsPreferences: ValidatorsPreferencesRemote? = null
    private val validatorsMutex = Mutex()

    override suspend fun getRecommendedValidatorIds(chainId: ChainId): Set<String> {
        return getValidators().preferred[chainId].orEmpty()
    }

    override suspend fun getExcludedValidatorIds(chainId: ChainId): Set<String> {
        return getValidators().excluded[chainId].orEmpty()
    }

    private suspend fun getValidators(): ValidatorsPreferencesRemote {
        return validatorsMutex.withLock {
            if (validatorsPreferences == null) {
                validatorsPreferences = fetchValidators()
            }

            validatorsPreferences ?: ValidatorsPreferencesRemote(emptyMap(), emptyMap())
        }
    }

    private suspend fun fetchValidators(): ValidatorsPreferencesRemote? {
        return runCatching {
            val chainsById = chainRegistry.chainsById()
            val preferences = validatorsApi.getValidators()
            val recommended = preferences.preferred.mapValuesNotNull { (chainId, addresses) ->
                chainsById[chainId]?.convertAddressesToAccountIds(addresses)
            }
            val excluded = preferences.excluded.mapValuesNotNull { (chainId, addresses) ->
                chainsById[chainId]?.convertAddressesToAccountIds(addresses)
            }

            ValidatorsPreferencesRemote(recommended, excluded)
        }.getOrNull()
    }

    private fun Chain.convertAddressesToAccountIds(addresses: Set<String>): Set<String> {
        return addresses.mapNotNullToSet {
            this.tryConvertAddressToAccountIdHex(it)
        }
    }

    private fun Chain.tryConvertAddressToAccountIdHex(address: String): String? {
        return runCatching { accountIdOf(address).toHexString() }.getOrNull()
    }
}
